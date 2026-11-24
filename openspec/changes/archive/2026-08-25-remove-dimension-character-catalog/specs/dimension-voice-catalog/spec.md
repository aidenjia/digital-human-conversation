## MODIFIED Requirements

### Requirement: List enabled dimension voices

系统 SHALL 提供 `GET /api/dimension-voices`（JWT Bearer）。Query 角色 `businessCode` 为**可选**。系统 MUST 仅返回 `dimension_voice.enabled=true` 的预设音色，并按 `businessCode` 升序排列（MUST NOT 按角色裁剪条目）。每项 MUST 包含 `businessCode`、`name`、`description`、`source`（固定 `preset`）、`used`。未传角色 `businessCode`（或空白）时，系统 MUST 返回完整启用预设列表且每项 `used=false`（预览），MUST NOT 校验角色目录。传入非空 `businessCode` 时，系统 MUST NOT 校验全局角色目录；`used=true` MUST 仅当：query 角色等于当前用户启用配置的 `character_business_code`，且该配置 `voice_source` 为 preset、`voice_id` 对应该音色行。从未保存、配置未绑定角色、query 角色不等于用户行角色、或保存为本地音色时，列表 MUST 全部 `used=false`。系统 MUST NOT 返回定制音色，MUST NOT 将腾讯 VoiceType 作为调用方选型键，MUST NOT 将雪花主键作为选型键。

#### Scenario: Authenticated list returns enabled preset voices

- **WHEN** 已鉴权客户端调用 `GET /api/dimension-voices?businessCode=` 且 query 角色等于用户已保存角色
- **THEN** 系统 MUST 返回 `ApiResponse` 且 `data` 为启用预设音色数组，且每项含 `used`

#### Scenario: Selected preset is marked used for the bound character

- **WHEN** query 角色等于当前用户已保存 `character_business_code` 且 `voice_id` 指向某启用预设
- **THEN** 该项 `used` MUST 为 true，其余 MUST 为 false

#### Scenario: Other character query leaves all used false

- **WHEN** query 角色非空但不等于当前用户已保存 `character_business_code`
- **THEN** 列表每项 `used` MUST 为 false

#### Scenario: Local voice save leaves all used false

- **WHEN** 当前用户配置为本地音色且 query 角色等于用户行角色
- **THEN** 列表每项 `used` MUST 为 false

#### Scenario: Preview without role businessCode

- **WHEN** 已鉴权客户端调用 `GET /api/dimension-voices` 且未传角色 `businessCode`（或空白）
- **THEN** 系统 MUST 返回完整启用预设列表且每项 `used` MUST 为 false

#### Scenario: Unknown role code does not fail

- **WHEN** query 传入非空但用户未保存或不匹配的角色 `businessCode`
- **THEN** 系统 MUST 返回 200 且列表每项 `used` MUST 为 false

#### Scenario: Disabled voices are omitted

- **WHEN** 库中存在 `enabled=false` 的次元预设音色
- **THEN** 列表 MUST NOT 包含该项

#### Scenario: Unauthenticated list is rejected

- **WHEN** 未携带有效 JWT 调用 `GET /api/dimension-voices`
- **THEN** 系统 MUST 拒绝请求（401 或项目等价）
