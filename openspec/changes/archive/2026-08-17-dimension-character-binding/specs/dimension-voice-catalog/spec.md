## MODIFIED Requirements

### Requirement: List enabled dimension voices

系统 SHALL 提供 `GET /api/dimension-voices`（JWT Bearer）。Query 角色 `businessCode` MUST 必填且非空。系统 MUST 校验该 code 对应恰好一条 `dimension_character.enabled=true` 的角色（`business_code` 唯一）；未知、未启用或不唯一 MUST 返回 400。系统 MUST 仅返回 `dimension_voice.enabled=true` 的预设音色，并按 `businessCode` 升序排列（MUST NOT 按角色裁剪条目）。每项 MUST 包含 `businessCode`、`name`、`description`、`used`。`used=true` MUST 仅当：query 角色等于当前用户启用配置所绑定的角色，且该配置 `voice_source` 为 preset、`voice_id` 对应该音色行。从未保存、配置未绑定角色、绑定的是其他角色、或保存为本地音色时，列表 MUST 全部 `used=false`。系统 MUST NOT 返回定制音色，MUST NOT 将腾讯 VoiceType 作为调用方选型键，MUST NOT 将雪花主键作为选型键。

#### Scenario: Authenticated list returns enabled preset voices

- **WHEN** 已鉴权客户端调用 `GET /api/dimension-voices?businessCode=` 且角色启用
- **THEN** 系统 MUST 返回 `ApiResponse` 且 `data` 为启用预设音色数组，且每项含 `used`

#### Scenario: Selected preset is marked used for the bound character

- **WHEN** query 角色等于当前用户已绑定角色且 `voice_id` 指向某启用预设
- **THEN** 该项 `used` MUST 为 true，其余 MUST 为 false

#### Scenario: Other character query leaves all used false

- **WHEN** query 角色启用但不等于当前配置绑定的角色
- **THEN** 列表每项 `used` MUST 为 false

#### Scenario: Local voice save leaves all used false

- **WHEN** 当前用户配置为本地音色且 query 角色等于绑定角色
- **THEN** 列表每项 `used` MUST 为 false

#### Scenario: Missing role businessCode fails

- **WHEN** 未传 `businessCode` 或传空白
- **THEN** 系统 MUST 返回 400

#### Scenario: Unknown or disabled character fails

- **WHEN** query 传入非空但未知或未启用的角色 `businessCode`
- **THEN** 系统 MUST 返回 400

#### Scenario: Disabled voices are omitted

- **WHEN** 库中存在 `enabled=false` 的次元预设音色
- **THEN** 列表 MUST NOT 包含该项

#### Scenario: Unauthenticated list is rejected

- **WHEN** 未携带有效 JWT 调用 `GET /api/dimension-voices`
- **THEN** 系统 MUST 拒绝请求（401 或项目等价）
