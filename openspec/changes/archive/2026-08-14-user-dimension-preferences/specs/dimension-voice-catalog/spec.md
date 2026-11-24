## ADDED Requirements

### Requirement: List enabled dimension voices

系统 SHALL 提供 `GET /api/dimension-voices`（JWT Bearer）。系统 MUST 仅返回 `dimension_voice.enabled=true` 的预设音色，并按 `businessCode` 升序排列。每项 MUST 包含 `businessCode`、`name`、`description`、`used`。`used=true` MUST 仅当当前用户（JWT `sub`）的启用 `user_dimension_config` 选中该预设音色（`voice_source` 为 preset 且 `voice_id` 对应该行）。从未保存、或保存为本地音色时，列表 MUST 全部 `used=false`。系统 MUST NOT 返回定制音色，MUST NOT 要求角色 query，MUST NOT 将腾讯 VoiceType 作为调用方选型键，MUST NOT 将雪花主键作为选型键。

#### Scenario: Authenticated list returns enabled preset voices

- **WHEN** 已鉴权客户端调用 `GET /api/dimension-voices`
- **THEN** 系统 MUST 返回 `ApiResponse` 且 `data` 为启用预设音色数组，且每项含 `used`

#### Scenario: Selected preset is marked used

- **WHEN** 当前用户已保存启用配置且 `voice_id` 指向某启用预设
- **THEN** 该项 `used` MUST 为 true，其余 MUST 为 false

#### Scenario: Local voice save leaves all used false

- **WHEN** 当前用户配置为本地音色
- **THEN** 列表每项 `used` MUST 为 false

#### Scenario: Disabled voices are omitted

- **WHEN** 库中存在 `enabled=false` 的次元预设音色
- **THEN** 列表 MUST NOT 包含该项

#### Scenario: Unauthenticated list is rejected

- **WHEN** 未携带有效 JWT 调用 `GET /api/dimension-voices`
- **THEN** 系统 MUST 拒绝请求（401 或项目等价）
