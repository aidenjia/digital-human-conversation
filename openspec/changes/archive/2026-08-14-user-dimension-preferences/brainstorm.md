## Design Summary

为 Jupiter SR「次元数字人」提供与陪伴隔离的用户偏好：独立预设音色目录、按 `userId` 一份配置（人设 / 音色 / 微调 / 欢迎语 / 安抚，无静默退出），配置 API 完全独立；进房仍走现有 `POST /api/convoai/agents`（`digitalHumanType=MMD`、不传陪伴 `businessCode`）。不改 `companion` 表、不建角色目录表、不接定制音色。

英文标识 `dimension` 仅作包名 / 表前缀 / URL 前缀（对应产品「次元」），不是声网术语。

## Alternatives Considered

### Alternative A: `companion.product_line` + 扩展 `user_companion_config`
- **Approach**: 给陪伴角色表打产品线，用户配置加 `persona_text`，列表按 query 过滤。
- **Pros**: 少建表；join 解析可复用陪伴 Service。
- **Cons**: 陪伴 `used` 与 Redis `companion:{sub}` 与次元串台；污染陪伴表；用户明确不要改 `companion`。
- **Why not chosen**: 产品线字段会改陪伴目录语义。

### Alternative B: 克隆陪伴四张表（含角色 + 定制音色）
- **Approach**: `dimension_character` / `dimension_voice` / `dimension_custom_voice` / `user_dimension_config`。
- **Pros**: 隔离最彻底。
- **Cons**: 次元无人设目录角色（人设绑 user）；无定制声需求；角色表 YAGNI。
- **Why not chosen**: 用户确认不需要角色表与定制音色表。

### Alternative C: 复用 `companion_voice`，只建 `user_dimension_config`
- **Approach**: 音色列表读陪伴预设表；用户配置按 `user_id` 一行。
- **Pros**: 少一张目录表。
- **Cons**: 陪伴启用集/文案变更会拖动次元列表。
- **Why not chosen**: 音色是产品目录，两入口需要独立预设表。

## Agreed Approach

**独立 `dimension_voice` + 按用户一行的 `user_dimension_config` + 独立 HTTP 路径 + join 按 MMD 读次元配置。** 陪伴 API / 表 / Redis key 零改动（join 仅增加 MMD 分支，缺省与带 `businessCode` 的陪伴行为保持不变）。

## Key Decisions

1. 不给 `companion` 加 `product_line`；不建 `dimension_character`；不建 / 不读 `companion_custom_voice`。
2. 人设绑定 JWT `sub`（`persona_text`），不绑定角色或 MMD 文件。
3. 表：新建 `dimension_voice`、`user_dimension_config`（`user_id` UNIQUE）。DDL 在共享 Postgres，本仓无 Flyway。
4. API 隔离：`GET /api/dimension-voices`；`GET/POST /api/user-dimension-configs*`；禁止扩展 `/api/companions*`。
5. 音色必选：`voiceSource=local` 或预设 `voiceBusinessCode`；无定制；未选 400。
6. 互动：欢迎语 / 安抚与陪伴同量纲与字数；**不**暴露或保存 idle / 静默退出；join `idle_timeout` 仍用全局 yml。
7. join：`digitalHumanType=MMD` 且无 `businessCode` 时加载次元配置；有 `businessCode` 仍走现有陪伴路径。
8. Redis：`digital-human:dimension:{sub}`，`instruction` = 用户人设（空则不覆盖 agent 默认）。agent 仓需能读该 key（跨仓，本 change 文档约定）。
9. 包：`api.dimension` / `dimension` / persistence；`companion` 包不引用次元。
10. 新路径 `authenticated()`；`user_id` 只来自 JWT `sub`。

## Open Questions

- 无（英文前缀暂定 `dimension`；人设长度上限实现时与陪伴 instruction 展示对齐，建议 ≤2000 字，写入 spec）。
