## ADDED Requirements

### Requirement: Upsert user dimension config on start save

系统 SHALL 提供 `POST /api/user-dimension-configs`（JWT Bearer）。系统 MUST 将 `user_id` 设为 JWT claim `sub`，MUST NOT 信任请求体中的 userId。成功时 MUST 对 `user_id` 执行 upsert（无则 INSERT，有则 UPDATE 同一行），并返回统一成功信封（`code=200`、`success=true`）；成功响应 MUST NOT 返回落库摘要 `data`。

音色选择（MUST 满足其一，无默认音色）：

- `voiceSource=local`：MUST NOT 依赖 `voiceBusinessCode`；落库 `voice_source=""`，`voice_id` 为 null
- 云端预设：非空 `voiceBusinessCode` 对应启用的 `dimension_voice`；`voiceSource` 为 `preset` 或省略

系统 MUST NOT 接受 `voiceSource=custom` 或定制音色选型字段。请求体 MUST NOT 包含陪伴角色 `businessCode`。成功落库后系统 MUST 将人设写入 Redis 逻辑 key `dimension:{sub}`（`digital-human` NameMapper 前缀），JSON MUST 含 `instruction`（非空 `personaText` 的 trim 值；空白则为空串）。MUST NOT 写入 `companion:{sub}`。Redis 失败 MUST NOT 阻止 upsert 成功。

MAY 包含 `speechRate`、`volume`、`pitch`、`greetingEnabled`、`greetingText`、`comfortEnabled`、`comfortText`、`personaText`。系统 MUST NOT 接受 idle / 静默退出时长字段。

#### Scenario: First save inserts one row per user

- **WHEN** 已鉴权用户首次提交合法次元配置（含合法音色）
- **THEN** 系统 MUST 插入一行 `user_dimension_config` 并返回 200 成功信封（无落库摘要 `data`）

#### Scenario: Successful upsert writes dimension persona cache

- **WHEN** 已鉴权用户成功保存次元配置
- **THEN** 系统 MUST 写入 `dimension:{sub}` 且 MUST NOT 修改 `companion:{sub}`

#### Scenario: Second save updates the same row

- **WHEN** 同一用户再次提交合法配置
- **THEN** 系统 MUST 更新已有行且 MUST NOT 插入第二行

#### Scenario: Local voice stores empty voice_source

- **WHEN** 请求 `voiceSource=local`
- **THEN** 落库 `voice_source` MUST 为空串且 `voice_id` MUST 为 null

#### Scenario: Missing voice selection fails

- **WHEN** 未传 `voiceSource=local` 且未提供非空 `voiceBusinessCode`
- **THEN** 系统 MUST 返回 400 且 MUST NOT 写库

#### Scenario: Custom voice is rejected

- **WHEN** 请求 `voiceSource=custom` 或携带定制音色 id
- **THEN** 系统 MUST 返回 400 且 MUST NOT 写库

---

### Requirement: Upsert MUST validate dimension voice and numeric ranges

系统 MUST 校验预设 `voiceBusinessCode` 存在于 `dimension_voice` 且 `enabled=true`。`speechRate` MUST > 0；`volume` MUST ∈ [0,100]；`pitch` MUST ≥ 1；缺省时 MUST 使用 1 / 50 / 100。`greetingText` 长度 MUST ≤ 100；`comfortText` 长度 MUST ≤ 50；`personaText` 长度 MUST ≤ 2000。本期 MUST NOT 做订阅权益校验。

#### Scenario: Unknown or disabled preset voice fails

- **WHEN** 指定预设音色不存在或 `enabled=false`
- **THEN** 系统 MUST 返回 400 且 MUST NOT 写库

#### Scenario: Persona text too long fails

- **WHEN** `personaText` 超过 2000 字
- **THEN** 系统 MUST 返回 400 且 MUST NOT 写库

---

### Requirement: Greeting comfort and persona texts are optional nulls

当请求未提供欢迎语/安抚/人设或文案为空白时，系统 MUST 将对应字段存为 `null`（MUST NOT 回落官方/yml 默认话术或陪伴角色 instruction）。欢迎语/安抚开关缺省 MUST 为 `true`。调用方提供的非空文案 MUST trim 后落库。

#### Scenario: Omitted persona stores null

- **WHEN** 启动保存未传 `personaText` 或传空白
- **THEN** 落库 `persona_text` MUST 为 `null`

#### Scenario: Omitted greeting text stores null

- **WHEN** 启动保存未传 `greetingText` 或传空白
- **THEN** 落库欢迎语文案 MUST 为 `null`

---

### Requirement: Audio interaction and persona echo latest save for the user

系统 SHALL 提供 `GET /api/user-dimension-configs/audio`、`GET /api/user-dimension-configs/interaction`、`GET /api/user-dimension-configs/persona`（JWT Bearer，无 Query）。系统 MUST 按 JWT `sub` 读取该用户启用配置（每用户至多一行）。`configured=false` MUST 表示从未成功保存；此时 MUST 返回 UI 默认锚点（微调 1/50/100；互动开关 true、文案 null；人设 null），MUST NOT 把锚点当成已落库值。`configured=true` MUST 返回落库值。interaction 响应 MUST NOT 包含 idle 或静默退出字段。

#### Scenario: Never saved returns configured false

- **WHEN** 用户无 `user_dimension_config` 行
- **THEN** 三个 GET MUST 返回 200 且 `configured=false`

#### Scenario: Saved returns configured true

- **WHEN** 用户已有启用配置
- **THEN** GET audio/interaction/persona MUST 返回 200 且 `configured=true` 及对应落库字段

#### Scenario: Unauthenticated config APIs are rejected

- **WHEN** 未携带有效 JWT 调用上述 GET 或 POST
- **THEN** 系统 MUST 拒绝请求（401 或项目等价）
