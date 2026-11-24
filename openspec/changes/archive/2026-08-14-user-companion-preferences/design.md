## Context

当前分支已具备 OpenID JWT Resource Server（`/api/convoai/**` authenticated）。共享 Postgres 已有 `companion`、`companion_voice`、`companion_custom_voice`、`user_companion_config`。产品配置页需要 per-user 偏好；登录由 Users 负责，本服务只验 JWT 并用 `sub`。

## Goals / Non-Goals

**Goals:**

- `GET /api/companions`、`GET /api/companion-voices`（可选角色 `businessCode`；启用目录；角色/音色均含 `used`；JWT）
- `GET /api/user-companion-configs/audio|interaction`（无角色 query；按 JWT `sub` 最近一次启动；`configured` 回显）
- `POST /api/user-companion-configs` upsert；`user_id`=JWT `sub`；音色必选（`local` / preset / custom）；成功仅信封（无 `data` 摘要）
- 定制选型：`voiceSource=custom` + `voiceBusinessCode`（非请求体 `customVoiceId`）
- 本地音色：请求 `local`，落库 `voice_source=""`；join TTS `voice_type` 用 yml
- join 可选 `businessCode`：合并用户配置注入 TTS / 欢迎语 / 安抚；idle 用全局 `convoai.idle-timeout`（默认 **180**）
- 欢迎语/安抚：开关默认 true；文案空白存 null（不回落官方默认）；字数 ≤100 / ≤50
- 更新 README 与对接文档；单测覆盖主路径

**Non-Goals:**

- 试听、订阅权益、建表 / Flyway、声音复刻支付
- 修改 Users 登录发 token
- 本地音色列表 API（由其他服务提供）

## Decisions

### 1. 身份

从 `SecurityContext` 的 `Jwt.getSubject()` 取 `user_id`；抽取小型 accessor，禁止请求体传 userId。

### 2. API 与包

- HTTP：`api.companion` + dto  
- 用例：`companion` + command + support（含 TTS 量纲映射）  
- 持久化：entity/mapper 对齐现表  
- 新路径加入 Resource Server `authenticated()`

### 3. upsert 校验顺序

Bearer → sub；`businessCode` 非空且角色启用；**音色必选**并校验；speechRate/volume/pitch 范围；文案字数；`(user_id, companion_id)` upsert。

### 4. join

可选 `businessCode`；有则查角色 + 用户配置注入；无配置行则 yml；本地/空 `voice_type` 用 yml 默认音色；无 `businessCode` 保持仅 yml。

### 5. 量纲

`speech_rate` 1→speed 0；volume 50→0；pitch 100→0（`TencentTtsParamMapper`）。

### 6. 已 supersede 的早期约定

- ~~省略音色 → 角色默认声 → `gentle_female`~~ → 未选音色 400
- ~~官方默认话术写入用户配置~~ → 空白存 null
- ~~idle 默认 300~~ → 180
- ~~响应暴露 `requiresSubscription`~~ → 不返回
- ~~定制仅 `customVoiceId`~~ → 以 `voiceBusinessCode` 为准

## Risks / Trade-offs

- [Risk] JWT `sub` 为空或格式不符 → Mitigation：明确 401/400，单测覆盖  
- [Risk] 前端仍传旧 `customVoiceId` 选型 → Mitigation：文档/Apifox 标明；服务端不提供兼容路径  
- [Risk] 本地选中无法从本服务音色列表 `used` 回显 → Mitigation：产品接受；本地列表在他服务  

## Migration Plan

- 表已存在则只发应用；新路径需 JWT  
- 客户端须改：upsert 必选音色；定制用 `voiceBusinessCode`；识别 local 响应 `voiceSource=""`  

## Open Questions

- 无。
