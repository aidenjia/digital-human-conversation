## Context

`digital-human-conversation` 已具备定制音色生产：`POST /corpus` 写私有桶并返回 `corpusUri`（对象键），提交时服务端 `GetObject` 再调 VRS。桶私有，客户端不能裸链播放。

部署：`replicas: 2`；中间件 Postgres + Redis + 腾讯 COS（密钥可与 VRS 回落共用）。`CustomVoiceCorpusKeys.belongsToUser` 已能校验对象键归属。

活规格：`openspec/specs/custom-voice-production/`（生产）；本 change 新增 `voice-corpus-preview`，不改生产 Requirements。

## Goals / Non-Goals

**Goals:**

- 提供 JWT 保护的语料**临时读 URL**（COS 预签名 GET）
- 两种入口：按 `corpusUri`；按生产单 `businessCode`（读库 `corpus_uri`）
- TTL 可配置；响应含 `url` + `expiresAt`
- 日志不输出完整签名 URL / 密钥

**Non-Goals:**

- 即时 TTS 合成试听、目录预设样音
- 前端直传 / STS 写凭证 / 公开桶
- 代理流式下载整文件经本服务
- 改 join、companion/dimension 列表字段、支付

## Decisions

### 1. 接口约定（前端契约）

| Method | Path | 入参 | 成功 `data` |
|--------|------|------|-------------|
| GET | `/api/custom-voices/corpus-url` | Query **必填** `corpusUri`（对象键） | `{ "url": "<presigned>", "expiresAt": "<ISO-8601>" }` |
| GET | `/api/custom-voices/{businessCode}/corpus-url` | Path `businessCode` | 同上 |

**公共约定**

- Header：`Authorization: Bearer <access token>`；用户 = JWT `sub`
- 信封：统一 `ApiResponse`（`code` / `success` / `message` / `data` / `error`）
- `url`：完整 HTTPS 预签名 GET，前端可直接给 `<audio src>` 或下载；**勿**当永久地址缓存超 TTL
- `expiresAt`：与签名过期时刻一致（建议 UTC，如 `2026-08-19T08:00:00Z`）
- 过期后：再调同一接口取新链接

**错误（对齐现有 custom-voices）**

| 情况 | HTTP / code | 说明 |
|------|-------------|------|
| 无/无效 token | 401 / 项目等价 | 未认证 |
| `corpusUri` 缺失或空白 | 400 | 校验失败 |
| `corpusUri` 不属于当前用户（前缀不符） | 400 或 403 | 禁止签他人对象 |
| `businessCode` 非本用户或不存在 | 403 或 404 | 与 status 轮询一致，不泄露 |
| 生产单 `corpus_uri` 为空 | 400 | 无法签名 |
| COS 密钥/桶未配置 | 503 | `ConfigurationException` |
| COS 签名上游失败 | 502 | `UpstreamException` |

**与现有路径关系**

```text
POST /corpus          → 写：返回 corpusUri
GET  /corpus-url      → 读：用 corpusUri 换临时 URL（试音）
POST /                → 提交制作
GET  /{businessCode}  → 轮询状态
GET  /{businessCode}/corpus-url → 用库内 corpus_uri 换临时 URL
```

Spring 字面路径 `/corpus-url` 与 `/{businessCode}` 并存；`/{businessCode}/corpus-url` 为子路径，无冲突。

**前端推荐流程**

```text
上传成功拿到 corpusUri
  → GET /corpus-url?corpusUri=...
  → audio.src = data.url
  → 播放失败/过期 → 重新 GET
```

制作中或完成后若只持有 `businessCode`，走第二接口即可。

### 2. 包与职责

- `api.customvoice`：薄 Controller + `CorpusPreviewResponse` DTO
- `customvoice`：`CustomVoiceCorpusPreviewService`（归属校验 + 调 COS + 算 expiresAt）
- `infrastructure.cos`：`TencentCosClient.presignGet`
- 不放入 `trtc`

### 3. COS 预签名

- 复用现有 `COSClient`；`GeneratePresignedUrlRequest`（HTTP GET）+ 过期 `Date`
- TTL：`tencent.cos.presign-ttl-seconds`，默认 `300`；非法/≤0 时实现可回落默认或 400（实现时钉死回落默认并打 warn）
- `corpusUri` 视为对象键，**禁止**接受完整 `http(s)` URL 当入参（有则 400）

### 4. 安全

- 必须 `belongsToUser` 或生产单归属通过后才签名
- 禁止 `..` 路径穿越（现有校验已含）
- 日志：objectKey、ttl、userId；MUST NOT 完整 `url`、SecretKey

### 5. 相对生产 change 的差异

- 生产 Non-goal 曾撤销 `corpus-url`；本期**独立 change** 正式落地，不 silent 改归档 design 的历史 Non-goal 叙述（可在 README 注明已提供试音）

## Risks / Trade-offs

- [Risk] 预签名 URL 在 TTL 内被转发 → Mitigation：短 TTL（默认 5 分钟）；仅所有者可签发
- [Risk] 时钟偏差导致提前过期 → Mitigation：前端以播放失败为准重签；`expiresAt` 供 UI 提示
- [Risk] 对象已被删 → Mitigation：签名仍可能成功，播放 404；前端提示重新上传（本接口不强制 HeadObject）
- [Risk] 与 `GET /{businessCode}` 路由混淆 → Mitigation：文档写清；测字面 `/corpus-url`

## Migration Plan

1. 发版即可；无 DDL
2. 客户端按需接入试音；不接则行为与现网一致
3. 回滚：下线新 mapping / 配置即可

## Open Questions

- （已决）双入口：`corpusUri` query + `businessCode` path
- （已决）默认 TTL 300s
- （实现时）`expiresAt` 序列化用 `Instant` ISO-8601
