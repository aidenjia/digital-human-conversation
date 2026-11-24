## MODIFIED Requirements

### Requirement: Upsert user dimension config on start save

系统 SHALL 提供 `POST /api/user-dimension-configs`（JWT Bearer）。系统 MUST 将 `user_id` 设为 JWT claim `sub`，MUST NOT 信任请求体中的 userId。请求体 MUST 包含次元角色 `businessCode`（来自 `GET /api/dimension-characters` 或客户端新建，不是音色 code、也不是陪伴角色 code）。系统 MUST 将该 code 解析为 `dimension_character` 并写入 `dimension_character_id`（逻辑引用，MUST NOT 建 DB 外键）：若存在且 `enabled=true` 则更新目录 `name`（请求非空时）与 `personaText`（请求必填）；若不存在则 MUST 创建启用角色（`business_code` 全局唯一）；创建时 MUST 提供非空 `name`；`personaText` trim 后长度 MUST > 1 且 ≤ 2000；若已存在但 `enabled=false` MUST 返回 400 且 MUST NOT 写库。成功时 MUST 对 `user_id` 执行 upsert（无则 INSERT，有则 UPDATE 同一行，换角色覆盖音色），并返回统一成功信封（`code=200`、`success=true`）；成功响应 MUST NOT 返回落库摘要 `data`。

音色选择（MUST 满足其一，无默认音色）：

- `voiceSource=local`：MUST NOT 依赖 `voiceBusinessCode`；落库 `voice_source=""`，`voice_id` 为 null
- 云端预设：非空 `voiceBusinessCode` 对应启用的 `dimension_voice`；`voiceSource` 为 `preset` 或省略

系统 MUST NOT 接受 `voiceSource=custom` 或定制音色选型字段。系统 MUST NOT 将人设作为 POST 入参校验或写入 `user_dimension_config`。成功落库后系统 MUST 将所选角色目录 `dimension_character.persona_text` 写入 Redis 逻辑 key `dimension:{sub}`（`digital-human` NameMapper 前缀），JSON MUST 含 `instruction`（trim 后的目录人设；空则为空串）。MUST NOT 写入 `companion:{sub}`。Redis 失败 MUST NOT 阻止 upsert 成功。

MAY 包含 `speechRate`、`volume`、`pitch`、`greetingEnabled`、`greetingText`、`comfortEnabled`、`comfortText`。系统 MUST NOT 接受 idle / 静默退出时长字段。

#### Scenario: First save inserts one row per user

- **WHEN** 已鉴权用户首次提交合法次元配置（含合法角色与音色）
- **THEN** 系统 MUST 插入一行 `user_dimension_config`（含 `dimension_character_id`）并返回 200 成功信封（无落库摘要 `data`）

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

#### Scenario: Unknown character requires name to create

- **WHEN** 角色 `businessCode` 在目录中不存在且未传非空 `name`
- **THEN** 系统 MUST 返回 400 且 MUST NOT 写库

#### Scenario: Unknown character with name creates catalog row

- **WHEN** 角色 `businessCode` 在目录中不存在且提供了非空 `name`
- **THEN** 系统 MUST 插入一条启用的 `dimension_character`（含非空 `persona_text`）并继续 upsert 用户配置

#### Scenario: Existing character updates catalog persona

- **WHEN** 角色 `businessCode` 已存在且 `enabled=true`，请求携带合法 `personaText`
- **THEN** 系统 MUST 更新 `dimension_character.persona_text`（及非空 `name` 若有变化）并继续 upsert 用户配置

#### Scenario: Unknown character requires personaText to create

- **WHEN** 角色 `businessCode` 在目录中不存在且未传非空 `personaText`（或 trim 后长度 ≤ 1）
- **THEN** 系统 MUST 返回 400 且 MUST NOT 写库

#### Scenario: Disabled character fails

- **WHEN** 角色 `businessCode` 已存在但 `enabled=false`
- **THEN** 系统 MUST 返回 400 且 MUST NOT 写库

#### Scenario: Missing voice selection fails

- **WHEN** 未传 `voiceSource=local` 且未提供非空 `voiceBusinessCode`
- **THEN** 系统 MUST 返回 400 且 MUST NOT 写库

#### Scenario: Custom voice is rejected

- **WHEN** 请求 `voiceSource=custom` 或携带定制音色 id
- **THEN** 系统 MUST 返回 400 且 MUST NOT 写库

---

### Requirement: Upsert MUST validate dimension voice and numeric ranges

系统 MUST 校验预设 `voiceBusinessCode` 存在于 `dimension_voice` 且 `enabled=true`。`speechRate` MUST > 0；`volume` MUST ∈ [0,100]；`pitch` MUST ≥ 1；缺省时 MUST 使用 1 / 50 / 100。`greetingText` 长度 MUST ≤ 100；`comfortText` 长度 MUST ≤ 50。本期 MUST NOT 做订阅权益校验。

#### Scenario: Unknown or disabled preset voice fails

- **WHEN** 指定预设音色不存在或 `enabled=false`
- **THEN** 系统 MUST 返回 400 且 MUST NOT 写库

---

## ADDED Requirements

### Requirement: Audio and interaction echo latest save for the user

系统 SHALL 提供 `GET /api/user-dimension-configs/audio`、`GET /api/user-dimension-configs/interaction`（JWT Bearer，无 Query）。系统 MUST 按 JWT `sub` 读取该用户启用配置（每用户至多一行）。`configured=false` MUST 表示从未成功保存；此时 MUST 返回 UI 默认锚点（微调 1/50/100；互动开关 true、文案 null），MUST NOT 把锚点当成已落库值。`configured=true` MUST 返回落库值。interaction 响应 MUST NOT 包含 idle 或静默退出字段。人设 MUST NOT 经本对端点回显（人设走 `GET /api/dimension-characters`）。

#### Scenario: Never saved returns configured false

- **WHEN** 用户无 `user_dimension_config` 行
- **THEN** GET audio 与 interaction MUST 返回 200 且 `configured=false`

#### Scenario: Saved returns configured true

- **WHEN** 用户已有启用配置
- **THEN** GET audio 与 interaction MUST 返回 200 且 `configured=true` 及对应落库字段

#### Scenario: Unauthenticated config APIs are rejected

- **WHEN** 未携带有效 JWT 调用上述 GET 或 POST
- **THEN** 系统 MUST 拒绝请求（401 或项目等价）

---

## REMOVED Requirements

### Requirement: Audio interaction and persona echo latest save for the user

**Reason**: 人设改为跟角色列表走；独立 `GET /api/user-dimension-configs/persona` 多余。

**Migration**: 客户端改用 `GET /api/dimension-characters` 每项的 `personaText`（角色目录人设）。audio / interaction 要求见 ADDED「Audio and interaction echo latest save for the user」。
