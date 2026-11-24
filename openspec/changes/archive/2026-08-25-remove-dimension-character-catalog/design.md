## Context

`digital-human-conversation` 为 ConvoAI HTTP 网关（K8s 多副本、无粘滞；Postgres + Redis）。此前 `dimension-character-binding` 引入 `dimension_character` 目录与 `dimension_character_id` 逻辑引用。现产品确认：次元角色由客户端定义，不需全局目录；人设写入用户配置行。

## Goals / Non-Goals

**Goals:**

- 删除 `dimension_character` 表及 `DimensionCharacter*`、`GET /api/dimension-characters`
- `user_dimension_config` 增加 `character_business_code` / `character_name` / `persona_text`；删除 `dimension_character_id`
- POST upsert 校验并写入三字段 + Redis `dimension:{sub}`
- `GET /api/dimension-voices`：可选 `businessCode`；未知 code 不 400；`used` 仅 query 等于用户行角色时标记
- 请求体不传场景字段；表不持久化 `scene_source` / `selected_scene_image_id`（后续列删除 DDL 另档）

**Non-Goals:**

- 新增 `GET /api/user-dimension-configs/persona`
- 陪伴侧 API / 表改动
- Flyway（共享库手工 DDL）
- 次元定制音色

## Decisions

1. **数据模型**：每用户一行；角色三字段跟用户行；`businessCode` 客户端自定义，**非**全局 UNIQUE。
2. **API**：删 `GET /dimension-characters`；POST 必填 `businessCode`、`name`、`personaText`；保留 `GET .../audio`、`GET .../interaction`。
3. **音色列表**：`listVoices(businessCode)` 可选；不查角色目录；`used` 逻辑读用户行 `character_business_code` + `voice_id`。
4. **鉴权**：`/api/dimension-characters` 下线；活规格 `openid-jwt-validation` 以 `/api/**` 通配覆盖，无需单独登记已删路径。
5. **迁移**：`docs/sql/drop-dimension-character.sql` 先回填三列再删列/表；先于应用发版执行。

## Risks / Trade-offs

- [Risk] 客户端仍调 `GET /dimension-characters` → Mitigation：破坏性说明；Apifox/README 同步删除
- [Risk] 旧行 `dimension_character_id` 无回填 → Mitigation：DDL UPDATE 从目录 JOIN 回填；无目录行则人工处理
- [Risk] 多用户相同 `businessCode` 语义混淆 → Mitigation：文档标明 code 仅用户行内语义，非全局主键

## Migration Plan

1. 共享 Postgres 执行 `drop-dimension-character.sql`
2. 发版应用（删代码路径）
3. Rollback：需恢复表与 API（本期不自动化回滚）
