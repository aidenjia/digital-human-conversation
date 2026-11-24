## Context

`digital-human-conversation` 已具备 JWT、陪伴目录、用户配置 upsert、join 按配置注入 TTS。`companion_custom_voice` 为用户级生产单表（无 `companion_id`）；已加 `training_text_id`、`provider_task_id`。桌面《声音复刻》描述腾讯 VRS 五步，但归属模型以现库为准。部署 `replicas: 2`，中间件含 Postgres 与 Redis。

## Goals / Non-Goals

**Goals:**

- 对齐产品一页「声音定制」：看文案 → **录音经后端上传 COS** → 提交建单并开做
- 生产接口：`training-text`、语料上传（后端收文件）、提交、轮询
- 直连腾讯 VRS **一句话复刻（TaskType=5）**；`businessCode=custom_00N`；`pay_status=2`
- 终态 `provider_voice_ref` 存数字 `VoiceType`；VRS **仅真实 Client**（禁止 Mock）

**Non-Goals:**

- 星核支付、试听 TTS API、前端直传 COS（临时凭证 / 浏览器 CORS 直传）
- 语料临时读 URL（`corpus-url`）；前端提交前本地播录音
- FastVoiceType 进 join / 声网 Gate
- 腾讯「基础版」长音频复刻路径
- 恢复 `companion_id`、新建枚举表、处理 `dimension_*` 表
- Flyway 强制；本仓库仍不负责建表

## Decisions

### 1. 用户流程与 API（已与产品对齐）

```
打开「声音定制」（不建单）
  → GET /api/custom-voices/training-text（文案）
  → 前端把录音交给后端 → 后端写入 COS，返回/记下 corpusUri
  → POST /api/custom-voices（建单 + 用音频调腾讯检测/训练）
  → GET /api/custom-voices/{businessCode} 轮询
```

| Method | Path | 作用 |
|--------|------|------|
| GET | `/api/custom-voices/training-text` | 看样例；返回 `text`+`textId` |
| POST | `/api/custom-voices/corpus` | 后端收录音 → PutObject → 返回 `corpusUri` |
| POST | `/api/custom-voices` | 提交：建单 + Detect + Create；返回 `businessCode` |
| GET | `/api/custom-voices/{businessCode}` | 轮询状态 |

**不做**：语料临时读 URL（前端提交前本地播录音；合成音色试听另议）。

**对外键** = `businessCode`（`custom_001` 起同用户递增）。技术 `id` 仅内部。

### 2. 包结构

- `api.customvoice`：Controller + DTO
- `customvoice`：Service + command + 枚举/映射
- `infrastructure`：Tencent VRS real client
- 不放入 `trtc`

### 3. 状态与写库

- 看样例：不写生产单（`textId` 由客户端回传）
- 提交：INSERT（`pay_status=2`，`training_text_id`，表单字段）→ 检测 → 成功则 `provider_task_id` + `status=1`
- 轮询：终态写 `provider_voice_ref`（数字 VoiceType）/ `fail_reason`

### 4. 腾讯映射（一句话）

- 全部调用 `TaskType=5`
- `domain` 业务 code → Domain 0～3（`companion` → 1）；Domain/TextLanguage 仅一句话生效
- `gender` 0女1男 → VoiceGender 2/1
- `source_language` → VoiceLanguage / TextLanguage
- 禁止业务字符串直传腾讯 Domain
- Detect：使用后端已收到的音频字节，或按 `corpus_uri` 从 COS GetObject 后再作为 `AudioData`
- Create：`SessionId` + `AudioIdList` + VoiceName/Gender/Language 等；可选 Codec/SampleRate
- Describe：成功取数字 `VoiceType` 写入 `provider_voice_ref`；忽略 FastVoiceType

### 5. 凭证与 COS

- VRS / COS 密钥走 Properties + 环境变量；禁止日志打印密钥与完整音频。
- **禁止 Mock**；缺 VRS 密钥 → ConfigurationException；缺 COS 配置 → **启动失败**。
- COS：私有桶；**后端 PutObject**；对象键 `{env}/custom-voice/{userId}/{uuid}.ext`（env=`local`/`dev`/`test`/`prod`）；桶名/地域配置外置。
- **不做**语料临时读链接；**不做**前端直传临时凭证接口。

### 6. 相对桌面方案的差异（钉死）

- 无 `companion_id` UNIQUE
- 不写 `companion.voice_id`
- 概览若需要：基于用户生产单 + 可选配置挂载，而非表上 companion 外键

### 7. 过期、额度与联调配置（已确认）

**过期时间**

- 本服务：**不**存、不配、不校验音色过期；轮询成功只写 `status` / `provider_voice_ref` / `fail_reason`。
- 腾讯：一句话结果可能含 `ExpireTime`；计费文档音色存储通常附赠约 3 个月免费期。本服务当前不消费该字段。

**免费额度（一句话，以腾讯控制台为准）**

- 免费音色约 5 个；合成约 1 万字符；领取起约 3 个月；每账号领取一次。与基础版资源包不共用。
- 文档：[计费概述](https://cloud.tencent.com/document/product/1283/93105)

**必须配置才能联调**

- 环境变量：`TENCENT_VRS_SECRET_ID` / `TENCENT_VRS_SECRET_KEY`；`COS_REGION` / `COS_BUCKET` / `COS_SECRET_ID` / `COS_SECRET_KEY`（COS **启动必填**，不回落 VRS）
- 子账号 CAM（如 `QcloudVRSFullAccess`）+ 主账号已开通声音复刻 + 目标桶 COS 读写
- 表 `companion_custom_voice` 列齐全（本仓库不 Flyway）
- K8s：日后在 CD 注入 `TENCENT_*`（当前 deployment 尚未包含）

**不做回顾**：合成 TTS 试听、`corpus-url`、Mock。

## Risks / Trade-offs

- [Risk] 与旧文档「基础版」字样混淆 → Mitigation：本 design / brainstorm 已钉死 TaskType=5；禁止再引入基础版路径
- [Risk] 训成后 VoiceType 为空仅有 FastVoiceType → Mitigation：成功态必须可 `parseInt`；否则写失败并记录 fail_reason
- [Risk] Redis AudioId 过期 → Mitigation：本期同请求 Detect→Create 可不依赖 Redis；跨请求再补
- [Risk] 双副本无粘滞 → Mitigation：状态全在 DB，无本地会话
- [Risk] 腾讯额度/权限不足 → Mitigation：失败写 `fail_reason`；联调用公司账号
- [Risk] Describe Status 整数与 StatusStr 文档示例不一致 → Mitigation：实现时以 StatusStr + 官方枚举双校验，常量集中

## Migration Plan

- 确认目标环境表已含两新列后再发版
- 新路径需 JWT；客户端按新 API 接入：录音先交后端上传 COS，再提交制作
- 回滚：停用新 Controller/配置即可；已写入的生产单数据保留

## Open Questions

- （已决）一句话 `TaskType=5`；仅 Real；**上传走后端**（非前端直传）
- （实现时核实）Describe 状态码；上传是独立 `/corpus` 还是 submit multipart 一次完成
