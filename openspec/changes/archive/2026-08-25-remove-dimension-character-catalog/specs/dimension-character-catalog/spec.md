## REMOVED Requirements

### Requirement: List enabled dimension characters

**Reason**: 次元角色改由客户端自定义并写入 `user_dimension_config` 用户行，不再维护全局 `dimension_character` 目录。

**Migration**: 停用 `GET /api/dimension-characters`。角色 `businessCode` / `name` / `personaText` 通过 `POST /api/user-dimension-configs` 写入；人设通过 Redis `dimension:{sub}.instruction` 供 join 使用，本期无 HTTP 人设回读端点。

#### Scenario: Dimension characters endpoint is removed

- **WHEN** 客户端调用 `GET /api/dimension-characters`
- **THEN** 系统 MUST 返回 404（或路由不存在）
