## Design Summary

`dimension_character` 全局目录与 `GET /api/dimension-characters` 增加了一轮间接层，但产品侧角色由客户端自定义、多用户可共用相同 `businessCode`，无需服务端维护全局唯一角色表。人设与角色身份迁回 `user_dimension_config` 用户行（`character_business_code` / `character_name` / `persona_text`）；POST upsert 直接写入并同步 Redis `dimension:{sub}.instruction`。

## Alternatives Considered

### Alternative A: 删除目录，人设跟用户行（选中）

- **Approach**: DROP `dimension_character`；删 `GET /dimension-characters`；POST 必填 `businessCode` + `name` + `personaText`；无 `GET .../persona`。
- **Pros**: 与客户端自定义角色模型一致；少一张表与目录同步逻辑。
- **Cons**: 无全局角色列表 API；换设备需依赖已保存配置或客户端本地草稿。
- **Why chosen**: 用户确认方案 A；实现与测试已落地。

### Alternative B: 保留目录，仅弱化校验

- **Pros**: 列表 API 仍在。
- **Cons**: 与「非全局目录」目标冲突；`business_code` UNIQUE 阻碍多用户同 code。
- **Why not chosen**: 与本期目标相反。

### Alternative C: 保留 `GET /persona` 回读人设

- **Pros**: 单独回读端点。
- **Cons**: POST 已写 `persona_text`；多一个 HTTP 面。
- **Why not chosen**: 本期不做人设 HTTP 回读；客户端用本地状态或后续再加专用 API。

## Decision

**删除 `dimension_character` 目录与相关 API；角色三字段落在 `user_dimension_config`；音色 `used` 按用户行 `character_business_code` 与可选 query 比对；不提供 `GET /user-dimension-configs/persona`。**
