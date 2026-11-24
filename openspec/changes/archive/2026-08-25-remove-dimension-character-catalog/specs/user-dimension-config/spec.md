## MODIFIED Requirements

### Requirement: Upsert user dimension config on start save

系统 SHALL 提供 `POST /api/user-dimension-configs`（JWT Bearer）。系统 MUST 将 `user_id` 设为 JWT claim `sub`，MUST NOT 信任请求体中的 userId。请求体 MUST 包含次元角色 `businessCode`（客户端自定义，不是音色 code、也不是陪伴角色 code）、非空 `name`、必填 `personaText`（trim 后长度 MUST > 1 且 ≤ 2000）。系统 MUST 将三者写入 `user_dimension_config.character_business_code`、`character_name`、`persona_text`（每用户一行，MUST NOT 维护全局角色目录）。成功时 MUST 对 `user_id` 执行 upsert（无则 INSERT，有则 UPDATE 同一行，换角色覆盖音色），并返回统一成功信封（`code=200`、`success=true`）；成功响应 MUST NOT 返回落库摘要 `data`。

音色选择（MUST 满足其一，无默认音色）：

- `voiceSource=local`：MUST NOT 依赖 `voiceBusinessCode`；落库 `voice_source=""`，`voice_id` 为 null
- 云端预设：`voiceSource=preset` 且非空 `voiceBusinessCode` 对应启用的 `dimension_voice`

系统 MUST NOT 接受 `voiceSource=custom` 或定制音色选型字段。请求体 MUST NOT 接受场景相关字段；`user_dimension_config` MUST NOT 持久化场景选用列。成功落库后系统 MUST 将 `persona_text` 写入 Redis 逻辑 key `dimension:{sub}`（`digital-human` NameMapper 前缀），JSON MUST 含 `instruction`（trim 后的人设；空则为空串）。MUST NOT 写入 `companion:{sub}`。Redis 失败 MUST NOT 阻止 upsert 成功。

MAY 包含 `speechRate`、`volume`、`pitch`、`greetingEnabled`、`greetingText`、`comfortEnabled`、`comfortText`。系统 MUST NOT 接受 idle / 静默退出时长字段。

#### Scenario: First save inserts one row per user

- **WHEN** 已鉴权用户首次提交合法次元配置（含合法角色与音色）
- **THEN** 系统 MUST 插入一行 `user_dimension_config`（含 `character_business_code`、`character_name`、`persona_text`）并返回 200 成功信封（无落库摘要 `data`）

#### Scenario: Successful upsert writes dimension persona cache

- **WHEN** 已鉴权用户成功保存次元配置
- **THEN** 系统 MUST 写入 `dimension:{sub}` 且 MUST NOT 修改 `companion:{sub}`

#### Scenario: Second save updates the same row

- **WHEN** 同一用户再次提交合法配置（含可能不同的角色 `businessCode`）
- **THEN** 系统 MUST 更新已有行且 MUST NOT 插入第二行

#### Scenario: Local voice stores empty voice_source

- **WHEN** 请求 `voiceSource=local` 且角色合法
- **THEN** 落库 `voice_source` MUST 为空串且 `voice_id` MUST 为 null

#### Scenario: Missing character businessCode fails

- **WHEN** 未传角色 `businessCode` 或传空白
- **THEN** 系统 MUST 返回 400 且 MUST NOT 写库

#### Scenario: Missing name fails

- **WHEN** 未传非空 `name`
- **THEN** 系统 MUST 返回 400 且 MUST NOT 写库

#### Scenario: Missing or invalid personaText fails

- **WHEN** 未传非空 `personaText`（或 trim 后长度 ≤ 1 或 > 2000）
- **THEN** 系统 MUST 返回 400 且 MUST NOT 写库

#### Scenario: Missing voice selection fails

- **WHEN** 未传非空 `voiceSource`，或未传 `voiceSource=local` 且未提供非空 `voiceBusinessCode`
- **THEN** 系统 MUST 返回 400 且 MUST NOT 写库

#### Scenario: Custom voice is rejected

- **WHEN** 请求 `voiceSource=custom` 或携带定制音色 id
- **THEN** 系统 MUST 返回 400 且 MUST NOT 写库

### Requirement: Audio and interaction echo latest save for the user

系统 SHALL 提供 `GET /api/user-dimension-configs/audio`、`GET /api/user-dimension-configs/interaction`（JWT Bearer，无 Query）。系统 MUST 按 JWT `sub` 读取该用户启用配置（每用户至多一行）。`configured=false` MUST 表示从未成功保存；此时 MUST 返回 UI 默认锚点（微调 1/50/100；互动开关 true、文案 null），MUST NOT 把锚点当成已落库值。`configured=true` MUST 返回落库值。interaction 响应 MUST NOT 包含 idle 或静默退出字段。系统 MUST NOT 提供 `GET /api/user-dimension-configs/persona`。

#### Scenario: Never saved returns configured false

- **WHEN** 用户无 `user_dimension_config` 行
- **THEN** GET audio 与 interaction MUST 返回 200 且 `configured=false`

#### Scenario: Saved returns configured true

- **WHEN** 用户已有启用配置
- **THEN** GET audio 与 interaction MUST 返回 200 且 `configured=true` 及对应落库字段

#### Scenario: Unauthenticated config APIs are rejected

- **WHEN** 未携带有效 JWT 调用上述 GET 或 POST
- **THEN** 系统 MUST 拒绝请求（401 或项目等价）

## REMOVED Requirements

### Requirement: Read dimension persona via dedicated GET

**Reason**: 人设写入用户行并由 POST upsert 维护；join 读 Redis。本期不提供 HTTP 人设回读。

**Migration**: 客户端在配置页使用本地状态或 POST 提交内容；勿再调用 `GET /api/user-dimension-configs/persona`。

#### Scenario: Persona GET is not available

- **WHEN** 客户端调用 `GET /api/user-dimension-configs/persona`
- **THEN** 系统 MUST 返回 404（或路由不存在）
