## ADDED Requirements

### Requirement: Upsert user companion config on start save

系统 SHALL 提供 `POST /api/user-companion-configs`（JWT Bearer）。请求体 MUST 包含非空 `businessCode`（**角色**业务编码）。系统 MUST 将 `user_id` 设为 JWT claim `sub`，MUST NOT 信任请求体中的 userId。成功时 MUST 对 `(user_id, companion_id)` 执行 upsert（无则 INSERT，有则 UPDATE），并返回统一成功信封（`code=200`、`success=true`）；成功响应 MUST NOT 返回落库摘要 `data`（字段可省略）。

音色选择（MUST 满足其一，**无**默认 `gentle_female`）：

- `voiceSource=local`：本地音色；MUST NOT 依赖 `voiceBusinessCode`；落库 `voice_source=""`，`voice_id` / `custom_voice_id` 为 null
- 云端预设：`voiceBusinessCode` 非空，且 `voiceSource` 为 `preset` 或省略
- 云端定制：`voiceSource=custom` + 非空 `voiceBusinessCode`（列表项业务编码如 `custom_001`，**不是**雪花 `id`）；按当前用户解析可用定制行

MAY 包含 `speechRate`、`volume`、`pitch`、`greetingEnabled`、`greetingText`、`comfortEnabled`、`comfortText`。请求体 MUST NOT 再接受以 `customVoiceId` 作为选型键的兼容路径。

#### Scenario: First save inserts row

- **WHEN** 已鉴权用户对某 `businessCode` 首次提交合法配置（含合法音色选择）
- **THEN** 系统 MUST 插入 `user_companion_config` 行并返回 200 成功信封（无落库摘要 `data`）

#### Scenario: Second save updates row

- **WHEN** 同一用户对同一角色再次提交合法配置
- **THEN** 系统 MUST 更新已有行且 MUST NOT 插入第二行

#### Scenario: Missing businessCode fails

- **WHEN** 请求体缺少非空 `businessCode`
- **THEN** 系统 MUST 返回 400 且 MUST NOT 写库

#### Scenario: Local voice stores empty voice_source

- **WHEN** 请求 `voiceSource=local`
- **THEN** 落库 `voice_source` MUST 为空串，两音色 id MUST 为 null；成功 HTTP 响应 MUST NOT 依赖 `data.voiceSource` 回显

#### Scenario: Missing voice selection fails

- **WHEN** 未传 `voiceSource=local` 且未提供非空 `voiceBusinessCode`
- **THEN** 系统 MUST 返回 400（`voice selection is required…`）且 MUST NOT 写库

---

### Requirement: Upsert MUST validate companion and voice listing

系统 MUST 校验角色按 `businessCode` 存在且启用。

- 预设：`voiceBusinessCode` MUST 对应启用的 `companion_voice`
- 定制：`voiceBusinessCode` MUST 对应当前用户可用定制音色（制作完成且支付成功）
- `speechRate` MUST > 0；`volume` MUST ∈ [0,100]；`pitch` MUST ≥ 1；缺省时 MUST 使用 1 / 50 / 100
- `greetingText` 长度 MUST ≤ 100；`comfortText` 长度 MUST ≤ 50（产品约定）
- 本期 MUST NOT 做订阅权益校验

#### Scenario: Unknown companion fails

- **WHEN** `businessCode` 无对应启用角色
- **THEN** 系统 MUST 返回 400 且 MUST NOT 写库

#### Scenario: Disabled or unknown preset voice fails

- **WHEN** 指定预设音色不存在或 `enabled=false`
- **THEN** 系统 MUST 返回 400 且 MUST NOT 写库

#### Scenario: Unavailable custom voice fails

- **WHEN** `voiceSource=custom` 且 `voiceBusinessCode` 对当前用户不可用
- **THEN** 系统 MUST 返回 400 且 MUST NOT 写库

---

### Requirement: Greeting and comfort texts are optional nulls

当请求未提供欢迎语/安抚文案或文案为空白时，系统 MUST 将对应字段存为 `null`（MUST NOT 回落官方/yml 默认话术写入用户配置）。开关缺省 MUST 为 `true`。调用方提供的非空文案 MUST trim 后落库。

#### Scenario: Omitted greeting text stores null

- **WHEN** 启动保存未传 `greetingText` 或传空白
- **THEN** 落库欢迎语文案 MUST 为 `null`

#### Scenario: Omitted comfort text stores null

- **WHEN** 启动保存未传 `comfortText` 或传空白
- **THEN** 落库安抚文案 MUST 为 `null`

---

### Requirement: Split GET for audio and interaction echo

系统 SHALL 提供 `GET /api/user-companion-configs/audio` 与 `GET /api/user-companion-configs/interaction`（JWT Bearer；无角色 query，按 JWT `sub` 取最近一次启动保存）。从未启动保存时 MUST 返回 HTTP 200 且 `configured=false`（附 UI 默认锚点/开关）。已保存时 MUST 返回 `configured=true` 与落库值。MUST NOT 再提供已删除的整包 `GET /api/user-companion-configs`。

#### Scenario: Never saved returns configured false

- **WHEN** 用户无任何启用配置行
- **THEN** audio/interaction GET MUST 返回 `configured=false`

#### Scenario: Saved returns configured true

- **WHEN** 用户已有启用配置（取 `updated_at` 最新一条）
- **THEN** audio/interaction GET MUST 返回 `configured=true` 与对应字段
