## ADDED Requirements

### Requirement: List enabled companions

系统 SHALL 提供 `GET /api/companions`（JWT Bearer）。系统 MUST 仅返回 `enabled=true` 的角色，并按 `businessCode` 升序排列。每项 MUST 包含 `businessCode`、`name`、`personaTag`、`instruction`、`digitalHumanType`、`used`。`used=true` MUST 仅对应当前用户最近一次启动保存（`user_companion_config.updated_at` 最新且 `enabled=true`）的角色；列表 MUST 至多一项为 `true`；从未保存时 MUST 全部为 `false`。MUST NOT 将雪花主键作为调用方定位键。

#### Scenario: Authenticated list returns enabled companions

- **WHEN** 已鉴权客户端调用 `GET /api/companions`
- **THEN** 系统 MUST 返回 `ApiResponse` 且 `data` 为启用角色数组，且每项含 `used`

#### Scenario: Companion used marks latest launch save

- **WHEN** 用户对多个角色有启用配置
- **THEN** 仅 `updated_at` 最新的那条配置对应角色 MUST 为 `used=true`，其余 MUST 为 `false`

#### Scenario: Unauthenticated list is rejected

- **WHEN** 未携带有效 JWT 调用 `GET /api/companions`
- **THEN** 系统 MUST 拒绝请求（401 或项目等价）

---

### Requirement: List companion voices for a role

系统 SHALL 提供 `GET /api/companion-voices`（JWT Bearer）。Query 角色 `businessCode` 为**可选**。系统 MUST 返回**合并列表**：启用预设音色 + 当前用户可用定制音色（制作完成且支付成功），预设按 `businessCode` 升序。每项 MUST 包含 `businessCode`、`name`、`description`、`source`（`preset`|`custom`）、`id`（定制雪花 id，预设为 null）、`used`（传入角色时：当前用户对该角色是否已选中该项云端音色；未传角色时全部为 false）。本期 MUST NOT 在响应中暴露 `requiresSubscription`。MUST NOT 将腾讯 VoiceType 作为调用方选型键；定制 upsert MUST 使用列表项 `businessCode`，MUST NOT 要求客户端传雪花 `id`。本列表 MUST NOT 包含本地音色。

#### Scenario: Authenticated list returns merged voices

- **WHEN** 已鉴权客户端调用 `GET /api/companion-voices?businessCode=` 且角色启用
- **THEN** 系统 MUST 返回启用预设与当前用户可用定制音色数组

#### Scenario: Preview without role code

- **WHEN** 已鉴权客户端调用 `GET /api/companion-voices` 且未传角色 `businessCode`（或空白）
- **THEN** 系统 MUST 仍返回启用预设与当前用户可用定制音色，且每项 `used` MUST 为 false

#### Scenario: Disabled preset voices are omitted

- **WHEN** 库中存在 `enabled=false` 的预设音色
- **THEN** 列表 MUST NOT 包含该音色

#### Scenario: Unknown role businessCode fails

- **WHEN** query 传入非空但未知或未启用的角色 `businessCode`
- **THEN** 系统 MUST 返回 400
