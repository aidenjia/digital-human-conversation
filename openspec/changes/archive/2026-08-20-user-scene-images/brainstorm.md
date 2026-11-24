## Design Summary

陪伴与次元配置页需要用户上传场景图（腾讯云 COS）、列出已上传图、并在启动保存时指定当前场景。场景图是**用户资产**，不绑角色：陪伴侧所有角色共用一套图库，次元侧单独一套（`scope=companion|dimension`）。选用并入现有 config upsert，语义对齐音色：`sceneSource=local` 表示客户端本地场景（清空云端选用）；`cloud` + `sceneImageId` 表示使用 COS 图。场景**必选**，不允许空。删除为硬删（库行 + COS 对象）；当前无数量上限。库表用行式资产表 + config 上的选用列；JSONB 仅作扩展元数据，不用 JSON 存图列表。

## Alternatives Considered

### Alternative A: 配置表塞 JSONB

- **Approach**: 在 `user_companion_config` / `user_dimension_config` 上挂 `scene_images jsonb` + 当前选用 id。
- **Pros**: 少一张表；读配置一次拿到列表。
- **Cons**: 并发上传读改写整段 JSON 易丢更新；配额/删除/索引难做；与现有用户资产表风格不一致。
- **Why not chosen**: 场景图会频繁增删改引用，不适合文档型 JSON 主模型。

### Alternative B: 统一资产表 + config 挂当前选用

- **Approach**: 新建 `user_scene_image`（`user_id` + `scope`）；两张 config 加 `scene_source` 与 `selected_scene_image_id`。
- **Pros**: 对齐 `companion_custom_voice`；列表/删/选用清晰；无 DB 外键；JSON 仅 `metadata`。
- **Cons**: 多一张表、列表多一次查询。
- **Why not chosen**: （选中）这是约定代价，可接受。

### Alternative C: 陪伴/次元各一张场景表

- **Approach**: `user_companion_scene` 与 `user_dimension_scene` 字段分叉。
- **Pros**: 边界极清晰。
- **Cons**: API/Service/Mapper 几乎双份；本期字段相同。
- **Why not chosen**: YAGNI。

## Agreed Approach

**Alternative B**：`user_scene_image` 按 `user_id` + `scope` 共享图库（不绑 `companion_id`）；选用写入现有 upsert；local 对齐音色空串落库。

## Key Decisions

1. 归属：`user_id` + `scope`（`companion` | `dimension`）；**不绑角色**。陪伴各角色可选用同一图库中不同图。
2. 上传：本服务代理（multipart → COS → 写库）；前端不直传。
3. COS URL：桶/前缀公有读或 CDN；落库稳定 `url`；前端直接 `<img src>`。库列名 `name`/`url` 不是 Postgres 保留字，沿用。
4. 场景名称：`user_scene_image.name` **必填**（trim 后非空）。
5. 选用并入现有 `POST /api/user-companion-configs` 与 `POST /api/user-dimension-configs`：`sceneSource=local` 或 `cloud` + `sceneImageId`；**不允许空**。
6. 落库：local → `scene_source=''` 且 `selected_scene_image_id IS NULL`；cloud → `scene_source='cloud'` 且 id 非空。DB CHECK 一次写对（避免音色 local 的 CHECK 漂移）。
7. 删除：**硬删**；若被任意同 scope config 引用，同一事务把那些行回退为 local，再删行与 COS。COS 删失败打 WARN（库已无引用）。
8. 数量上限：本期不做。
9. 列表 `used`：对照当前用户「使用中」配置（陪伴 = 最近一次启动保存行；次元 = 唯一行）；local 时全部 `used=false`。
10. Non-goals：缩略图、私有桶预签名读、平台预设场景库、前端直传、数量上限、软删、Flyway（本仓无）。

## Open Questions

- 无（陪伴与次元图库分开；公有读 URL；`name`/`url` 列名保留）。
