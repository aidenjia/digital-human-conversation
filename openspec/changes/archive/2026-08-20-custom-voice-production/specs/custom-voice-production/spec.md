## ADDED Requirements

### Requirement: Fetch Tencent training text before order

系统 SHALL 提供 `GET /api/custom-voices/training-text`（JWT Bearer）。此接口 MUST NOT 创建 `companion_custom_voice` 生产单。Query MAY 含 `sourceLanguage`（`zh-CN`|`en-US`|`ja-JP`，缺省 `zh-CN`）与 `domain`（`companion`|`customer_service`|`marketing`|`news`，缺省 `companion`）。系统 MUST 经 VRS 客户端以**一句话复刻 TaskType=5** 获取训练文案（业务 code MUST 由服务端映射为腾讯 Domain/TextLanguage，MUST NOT 把如 `companion` 直传腾讯 Domain）。成功时 `data` MUST 返回朗读 `text` 与 `textId`。客户端 MUST 在最终提交时回传同一 `textId`。

本接口对应产品「查看文案样例」：用户打开定制弹窗后即可调用，MUST NOT 要求先点提交或先建单。

#### Scenario: Fetch training text without creating order

- **WHEN** 已鉴权用户以合法 language/domain（或缺省）请求 training-text
- **THEN** 系统 MUST 返回 `text` 与 `textId`，且 MUST NOT INSERT `companion_custom_voice`

#### Scenario: Invalid language or domain fails

- **WHEN** `sourceLanguage` 或 `domain` 不合法
- **THEN** 系统 MUST 返回 400

#### Scenario: Missing token fails

- **WHEN** 请求缺少有效 Bearer token
- **THEN** 系统 MUST 拒绝未认证请求

---

### Requirement: Submit custom voice production after recording

系统 SHALL 提供 `POST /api/custom-voices`（JWT Bearer）作为最终提交。系统 MUST 将 `user_id` 设为 JWT `sub`。请求 MUST 包含：非空 `name`、合法 `gender`（0/1）、`sourceLanguage`、`domain`、非空 `textId`，以及已由后端上传产生的非空 `corpusUri`（或实现时允许同请求携带音频文件并由服务端先上传再检测）。MAY 含 `description`、`contactPhone`、`codec`、`sampleRate`。请求体 MUST NOT 由客户端指定 `businessCode`。

成功时系统 MUST：

1. INSERT 生产单：`business_code=custom_00N`；`pay_status=2`；写入 `training_text_id`、`corpus_uri` 与表单字段  
2. 取得语料字节（上传时缓存或从 COS GetObject）后调用腾讯一句话检测；失败则写 `status=3`/`fail_reason`，MUST NOT 创建训练  
3. 检测通过后 CreateVRSTask：写 `provider_task_id`，`status=1`  

成功响应 `data` MUST 含 `businessCode`。

#### Scenario: Submit creates order and starts training

- **WHEN** 语料已由后端写入 COS 且提交合法、检测与建任务成功
- **THEN** 系统 MUST 插入 `custom_00N` 行，`status` MUST 为 1，并返回 `businessCode`

#### Scenario: Submit without textId fails

- **WHEN** 缺少 `textId`
- **THEN** 系统 MUST 返回 400 且 MUST NOT 创建训练任务

#### Scenario: Detection failure does not train

- **WHEN** 腾讯检测未通过
- **THEN** 系统 MUST 返回失败信息且 MUST NOT 创建训练任务

#### Scenario: Business code increments per user

- **WHEN** 同一用户第二次提交成功建单
- **THEN** 新单 `business_code` MUST 为下一序号

---

### Requirement: Query custom voice production status

系统 SHALL 提供 `GET /api/custom-voices/{businessCode}`（JWT Bearer）。系统 MUST 按 `(user_id=sub, business_code)` 定位。若 `status=1` 且存在 `provider_task_id`，MUST 查询腾讯任务状态并在终态回写：成功 `status=2` + `provider_voice_ref`（MUST 为可 `Integer.parseInt` 的数字 VoiceType；MUST NOT 将 FastVoiceType 写入该字段）；失败 `status=3` + `fail_reason`。`data` MUST 含 `businessCode`、`status`、`providerTaskId`、`providerVoiceRef`、`failReason`。

#### Scenario: Polling completes successfully

- **WHEN** 所有者查询制作中单且腾讯成功
- **THEN** 系统 MUST 置 `status=2` 并持久化数字 `provider_voice_ref`

#### Scenario: Non-owner cannot query

- **WHEN** 用户查询不属于自己的 `businessCode`
- **THEN** 系统 MUST 返回 403 或 404

---

### Requirement: VRS real client only

系统 MUST 仅提供真实腾讯 VRS 客户端（**禁止** Mock / 假成功路径）。MUST 使用腾讯凭证调用一句话四接口：`GetTrainingText`、`DetectEnvAndSoundQuality`、`CreateVRSTask`、`DescribeVRSTaskStatus`。密钥 MUST 外置；日志 MUST NOT 输出 SecretKey 或完整音频 Base64。SecretId/SecretKey 缺失时 MUST 以配置错误失败。

#### Scenario: Missing credentials fails

- **WHEN** SecretId 或 SecretKey 缺失且调用 VRS
- **THEN** 系统 MUST 以配置错误失败（MUST NOT 返回模拟文案或假任务）

---

### Requirement: Backend corpus upload to COS

系统 SHALL 提供 JWT 保护的语料上传能力（独立路径或并入提交 multipart，实现时钉死）。客户端 MUST 将录音文件提交给本服务；系统 MUST 使用服务端 COS 凭证 `PutObject` 写入私有桶，对象键 MUST 落在可配置前缀下（联调默认 `test/custom-voice/{userId}/{uuid}.ext`），并返回 `corpusUri`。系统 MUST NOT 向客户端下发前端直传临时凭证。

#### Scenario: Upload stores object under configured prefix

- **WHEN** 已鉴权用户上传合法音频
- **THEN** 系统 MUST 将对象写入配置前缀下（含当前 `userId` 分段）并返回 `corpusUri`

#### Scenario: Missing COS credentials fails

- **WHEN** COS 密钥或桶配置缺失
- **THEN** 系统 MUST 在启动阶段失败（不得以半配置状态对外服务）

---

### Requirement: Custom voice production APIs use unified envelope and auth

所有 `/api/custom-voices/**` 接口 MUST 要求 JWT Bearer，MUST 使用统一 `ApiResponse` 信封。日志 MUST NOT 输出 Authorization、腾讯 SecretKey、COS 永久密钥、或完整音频 Base64。

#### Scenario: Unauthorized calls fail

- **WHEN** 无 token 调用任一生产 API
- **THEN** 系统 MUST 拒绝未认证请求
