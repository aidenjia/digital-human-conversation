## Design Summary

先前把次元人设/音色挂在 `userId` 上（不建角色目录）是误判。对照 [Jupiter SR 原型](https://docs.example.test/dimension-character-binding)，人设与音色必须跟**当前选中的次元角色**走；微调与互动仍是用户级、不跟角色。

产品约束：一个用户**同时**只能配置一个角色、一个音色、一套微调/互动/人设。因此不按 `(user × 角色)` 分存历史；换角色即 UPDATE 同一行（旧角色人设/音色被覆盖）。

落地形态：新建 `dimension_character` 目录；`user_dimension_config` 增加逻辑列 `dimension_character_id`（**无 DB 外键**）；`GET /api/dimension-characters` 回显 `businessCode`、`name`、`used`、`personaText`（仅 `used` 项有人设）；删除 `GET .../persona`；音色列表必带角色 `businessCode` 才能标 `used`。

## Alternatives Considered

### Alternative A: `(user × 角色)` 两张配置表

- **Approach**: `user_dimension_config` 只留微调/互动；另建 `(user_id, character_id)` 存人设+音色。
- **Pros**: 换角色能找回上一份人设/音色。
- **Cons**: 与「同时只有一份配置」矛盾；多一次 upsert、多一张表。
- **Why not chosen**: 用户确认同时只能一个角色；分表是过度设计。

### Alternative B: 现有用户行加 `dimension_character_id`（无 FK）

- **Approach**: 保持 `user_id` UNIQUE；目录表校验 `business_code` 存在且唯一；应用层写入角色主键，库不建 FOREIGN KEY。
- **Pros**: 最小 diff；微调/互动自然不跟角色；与陪伴「不使用 DB 外键」约定一致。
- **Cons**: 换角色覆盖旧人设/音色。
- **Why not chosen**: （选中）这就是约定代价。

### Alternative C: 只加 `character_id` 但不建目录表

- **Approach**: 客户端自报角色 code，配置表存字符串。
- **Pros**: 少一张表。
- **Cons**: 无法校验 `business_code` 唯一/启用；列表无从提供 `name`。
- **Why not chosen**: 角色列表与唯一性校验都需要目录。

## Agreed Approach

**Alternative B**：`dimension_character` 目录 + 现有 `user_dimension_config` 加逻辑 `dimension_character_id`（无 FK）。人设挂在角色列表接口上，不再单独 GET persona。

## Key Decisions

1. 纠正 2026-08-14 决策：次元需要角色目录；人设/音色跟当前角色，不是「纯 userId」。
2. 粒度仍是每用户一行（`user_id` UNIQUE），不是陪伴的 `(user × 角色)`。
3. **禁止 DB 外键**；`dimension_character_id` 为逻辑引用，存在性/启用/唯一性在应用层校验（对齐 `docs/user-companion-config-schema.md`）。
4. `GET /api/dimension-characters`：每项必有 `businessCode`、`name`；`used` 至多一项；`personaText` 仅 `used=true` 时为落库人设，其余 `null`。
5. **删除** `GET /api/user-dimension-configs/persona`。
6. `GET /api/dimension-voices?businessCode=` **必填**；`used=true` 仅当该角色等于当前配置角色且选中该预设。未知/未启用/不唯一 → 400。
7. `GET .../audio`、`GET .../interaction` 仍无 Query（微调/互动跟用户）。
8. `POST /api/user-dimension-configs` 必带次元角色 `businessCode` + `personaText` + 音色（local 或预设）；upsert 同一行。
9. join 仍 `digitalHumanType=MMD`、不传角色 code，读这一行；Redis `dimension:{sub}.instruction` 仍由 upsert 写入。
10. 不改陪伴表/API/Redis；无定制音色；无 idle。

## Open Questions

- 无（角色列表路径定为 `/api/dimension-characters`；旧配置行无 `dimension_character_id` 时角色/音色 `used` 与人设均为未选中，audio/interaction 仍可 `configured=true`）。
