## MODIFIED Requirements

### Requirement: Upsert user dimension config on start save

系统 SHALL 提供 `POST /api/user-dimension-configs`（JWT Bearer）。系统 MUST 将 `user_id` 设为 JWT claim `sub`，MUST NOT 信任请求体中的 userId。请求体 MUST 包含次元角色 `businessCode`（来自 `GET /api/dimension-characters` 或客户端新建，不是音色 code、也不是陪伴角色 code）。系统 MUST 将该 code 解析为 `dimension_character` 并写入 `dimension_character_id`（逻辑引用，MUST NOT 建 DB 外键）：若存在且 `enabled=true` 则更新目录 `name`（请求非空时）与 `personaText`（请求必填）；若不存在则 MUST 创建启用角色（`business_code` 全局唯一）；创建时 MUST 提供非空 `name`；`personaText` trim 后长度 MUST > 1 且 ≤ 2000；若已存在但 `enabled=false` MUST 返回 400 且 MUST NOT 写库。成功时 MUST 对 `user_id` 执行 upsert（无则 INSERT，有则 UPDATE 同一行，换角色覆盖音色），并返回统一成功信封（`code=200`、`success=true`）；成功响应 MUST NOT 返回落库摘要 `data`。

音色选择（MUST 满足其一，无默认音色）：

- `voiceSource=local`：MUST NOT 依赖 `voiceBusinessCode`；落库 `voice_source=""`，`voice_id` 为 null
- 云端预设：`voiceSource=preset` 且非空 `voiceBusinessCode` 对应启用的 `dimension_voice`

场景选择（MUST 满足其一，**不允许空**）：

- `sceneSource=local`：客户端本地场景；MUST NOT 携带 `sceneImageId`；落库 `scene_source=""`，`selected_scene_image_id` 为 null
- 云端图：`sceneSource=cloud`（或省略 source 但 `sceneImageId` 非空）+ 非空 `sceneImageId`；该行 MUST 属于当前用户且 `scope=dimension`

系统 MUST NOT 接受 `voiceSource=custom` 或定制音色选型字段。系统 MUST NOT 将人设作为 POST 入参校验或写入 `user_dimension_config`。成功落库后系统 MUST 将所选角色目录 `dimension_character.persona_text` 写入 Redis 逻辑 key `dimension:{sub}`（`digital-human` NameMapper 前缀），JSON MUST 含 `instruction`（trim 后的目录人设；空则为空串）。MUST NOT 写入 `companion:{sub}`。Redis 失败 MUST NOT 阻止 upsert 成功。

MAY 包含 `speechRate`、`volume`、`pitch`、`greetingEnabled`、`greetingText`、`comfortEnabled`、`comfortText`。系统 MUST NOT 接受 idle / 静默退出时长字段。

#### Scenario: First save inserts one row per user

- **WHEN** 已鉴权用户首次提交合法次元配置（含合法角色、音色与场景）
- **THEN** 系统 MUST 插入一行 `user_dimension_config`（含 `dimension_character_id`）并返回 200 成功信封（无落库摘要 `data`）

#### Scenario: Successful upsert writes dimension persona cache

- **WHEN** 已鉴权用户成功保存次元配置
- **THEN** 系统 MUST 写入 `dimension:{sub}` 且 MUST NOT 修改 `companion:{sub}`

#### Scenario: Second save updates the same row

- **WHEN** 同一用户再次提交合法配置（含可能不同的角色 `businessCode`）
- **THEN** 系统 MUST 更新已有行且 MUST NOT 插入第二行

#### Scenario: Local voice stores empty voice_source

- **WHEN** 请求 `voiceSource=local` 且角色与场景合法
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

- **WHEN** 未传非空 `voiceSource`，或未传 `voiceSource=local` 且未提供非空 `voiceBusinessCode`
- **THEN** 系统 MUST 返回 400 且 MUST NOT 写库

#### Scenario: Custom voice is rejected

- **WHEN** 请求 `voiceSource=custom` 或携带定制音色 id
- **THEN** 系统 MUST 返回 400 且 MUST NOT 写库

#### Scenario: Local scene stores empty scene_source

- **WHEN** 请求 `sceneSource=local` 且角色与音色合法
- **THEN** 落库 `scene_source` MUST 为空串且 `selected_scene_image_id` MUST 为 null

#### Scenario: Cloud scene stores selected image id

- **WHEN** 请求携带当前用户 dimension scope 下合法 `sceneImageId`
- **THEN** 落库 `scene_source` MUST 为 `cloud` 且 `selected_scene_image_id` MUST 等于该 id

#### Scenario: Missing scene selection fails

- **WHEN** 未传 `sceneSource=local` 且未提供非空 `sceneImageId`
- **THEN** 系统 MUST 返回 400（`scene selection is required…`）且 MUST NOT 写库

#### Scenario: Companion-scope scene image fails on dimension upsert

- **WHEN** `sceneImageId` 对应行的 `scope` 为 `companion`
- **THEN** 系统 MUST 返回 400 且 MUST NOT 写库
