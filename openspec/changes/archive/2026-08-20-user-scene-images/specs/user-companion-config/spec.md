## MODIFIED Requirements

### Requirement: Upsert user companion config on start save

系统 SHALL 提供 `POST /api/user-companion-configs`（JWT Bearer）。请求体 MUST 包含非空 `businessCode`（**角色**业务编码）。系统 MUST 将 `user_id` 设为 JWT claim `sub`，MUST NOT 信任请求体中的 userId。成功时 MUST 对 `(user_id, companion_id)` 执行 upsert（无则 INSERT，有则 UPDATE），并返回统一成功信封（`code=200`、`success=true`）；成功响应 MUST NOT 返回落库摘要 `data`（字段可省略）。

音色选择（MUST 满足其一，**无**默认 `gentle_female`）：

- `voiceSource=local`：本地音色；MUST NOT 依赖 `voiceBusinessCode`；落库 `voice_source=""`，`voice_id` / `custom_voice_id` 为 null
- 云端预设：`voiceBusinessCode` 非空，且 `voiceSource` 为 `preset` 或省略
- 云端定制：`voiceSource=custom` + 非空 `voiceBusinessCode`（列表项业务编码如 `custom_001`，**不是**雪花 `id`）；按当前用户解析可用定制行

场景选择（MUST 满足其一，**不允许空**）：

- `sceneSource=local`：客户端本地场景；MUST NOT 携带 `sceneImageId`；落库 `scene_source=""`，`selected_scene_image_id` 为 null
- 云端图：`sceneSource=cloud`（或省略 source 但 `sceneImageId` 非空）+ 非空 `sceneImageId`（`user_scene_image.id`，**不是**业务 code）；该行 MUST 属于当前用户且 `scope=companion`

MAY 包含 `speechRate`、`volume`、`pitch`、`greetingEnabled`、`greetingText`、`comfortEnabled`、`comfortText`。请求体 MUST NOT 再接受以 `customVoiceId` 作为选型键的兼容路径。

#### Scenario: First save inserts row

- **WHEN** 已鉴权用户对某 `businessCode` 首次提交合法配置（含合法音色与场景选择）
- **THEN** 系统 MUST 插入 `user_companion_config` 行并返回 200 成功信封（无落库摘要 `data`）

#### Scenario: Second save updates row

- **WHEN** 同一用户对同一角色再次提交合法配置
- **THEN** 系统 MUST 更新已有行且 MUST NOT 插入第二行

#### Scenario: Missing businessCode fails

- **WHEN** 请求体缺少非空 `businessCode`
- **THEN** 系统 MUST 返回 400 且 MUST NOT 写库

#### Scenario: Local voice stores empty voice_source

- **WHEN** 请求 `voiceSource=local` 且场景选择合法
- **THEN** 落库 `voice_source` MUST 为空串，两音色 id MUST 为 null；成功 HTTP 响应 MUST NOT 依赖 `data.voiceSource` 回显

#### Scenario: Missing voice selection fails

- **WHEN** 未传 `voiceSource=local` 且未提供非空 `voiceBusinessCode`
- **THEN** 系统 MUST 返回 400（`voice selection is required…`）且 MUST NOT 写库

#### Scenario: Local scene stores empty scene_source

- **WHEN** 请求 `sceneSource=local` 且音色选择合法
- **THEN** 落库 `scene_source` MUST 为空串，`selected_scene_image_id` MUST 为 null

#### Scenario: Cloud scene stores selected image id

- **WHEN** 请求携带当前用户 companion scope 下合法 `sceneImageId`
- **THEN** 落库 `scene_source` MUST 为 `cloud` 且 `selected_scene_image_id` MUST 等于该 id

#### Scenario: Missing scene selection fails

- **WHEN** 未传 `sceneSource=local` 且未提供非空 `sceneImageId`
- **THEN** 系统 MUST 返回 400（`scene selection is required…`）且 MUST NOT 写库

#### Scenario: Local scene rejects sceneImageId

- **WHEN** 请求 `sceneSource=local` 且同时携带非空 `sceneImageId`
- **THEN** 系统 MUST 返回 400 且 MUST NOT 写库

#### Scenario: Foreign or dimension-scope scene image fails

- **WHEN** `sceneImageId` 不属于当前用户，或不存在，或 `scope` 不是 `companion`
- **THEN** 系统 MUST 返回 400 且 MUST NOT 写库
