# Proposal: remove-dimension-character-catalog

## Why

次元角色不再维护全局目录表；人设与角色身份迁回每用户一行 `user_dimension_config`，对外删除 `GET /api/dimension-characters`。

## What

- 删除 `dimension_character` 表、`DimensionCharacter*` 实体/Mapper、`GET /api/dimension-characters`
- `user_dimension_config` 增加 `character_business_code` / `character_name` / `persona_text`；删除 `dimension_character_id`
- POST upsert 必填 `businessCode`、`name`、`personaText`，写入用户行并同步 Redis `dimension:{sub}.instruction`
- **不提供** `GET /api/user-dimension-configs/persona`（人设无 HTTP 回读端点）
- `GET /api/dimension-voices`：`businessCode` 可选；不校验角色目录；未知 code 返回 200 且 `used=false`
- 次元 POST **不接受** `sceneSource` / `sceneImageId`；表移除场景选用列

## Capabilities

### Removed Capabilities

- `dimension-character-catalog`

### Modified Capabilities

- `user-dimension-config`: 角色三字段跟用户行；删 persona GET
- `dimension-voice-catalog`: 可选 query；`used` 读用户行 `character_business_code`
- `openid-jwt-validation`: 下线 dimension-characters 路径登记（活规格 `/api/**`）

## Impact

- **破坏性**：客户端停用 `GET /api/dimension-characters`；勿再调 `GET /api/user-dimension-configs/persona`
- **DB**：共享 Postgres 执行 `docs/sql/drop-dimension-character.sql`（先于发版）
- **CD**：无 env 变更
