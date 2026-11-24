## ADDED Requirements

### Requirement: List enabled dimension characters

系统 SHALL 提供 `GET /api/dimension-characters`（JWT Bearer）。系统 MUST 仅返回 `dimension_character.enabled=true` 的角色，并按 `businessCode` 升序排列。每项 MUST 包含 `businessCode`、`name`、`used`、`personaText`。`businessCode` 与 `name` MUST 均非空。`used=true` MUST 仅当当前用户（JWT `sub`）的启用 `user_dimension_config.dimension_character_id` 对应该角色；列表 MUST 至多一项为 `true`。从未保存、或配置行尚未绑定角色时，MUST 全部 `used=false`。`personaText` MUST 来自 `dimension_character.persona_text`，每项 MUST 回显该角色目录人设（与 `used` 无关）。MUST NOT 将雪花主键作为调用方定位键。MUST NOT 在 `user_dimension_config` 存放人设。

#### Scenario: Authenticated list returns enabled characters

- **WHEN** 已鉴权客户端调用 `GET /api/dimension-characters`
- **THEN** 系统 MUST 返回 `ApiResponse` 且 `data` 为启用角色数组，且每项含 `businessCode`、`name`、`used`、`personaText`

#### Scenario: Each item includes catalog persona

- **WHEN** 目录中存在启用角色且各有 `persona_text`
- **THEN** 每项 `personaText` MUST 为对应角色目录人设，无论 `used` 为 true 或 false

#### Scenario: Bound character marks used

- **WHEN** 当前用户已保存启用配置且 `dimension_character_id` 指向某启用角色
- **THEN** 该项 `used` MUST 为 true；其余 MUST `used=false`；人设仍 MUST 来自各自目录行

#### Scenario: Unbound config leaves all unused

- **WHEN** 用户无配置，或配置行 `dimension_character_id` 为空
- **THEN** 列表每项 `used` MUST 为 false，且 `personaText` MUST 仍为该角色目录人设

#### Scenario: Disabled characters are omitted

- **WHEN** 库中存在 `enabled=false` 的次元角色
- **THEN** 列表 MUST NOT 包含该项

#### Scenario: Unauthenticated list is rejected

- **WHEN** 未携带有效 JWT 调用 `GET /api/dimension-characters`
- **THEN** 系统 MUST 拒绝请求（401 或项目等价）
