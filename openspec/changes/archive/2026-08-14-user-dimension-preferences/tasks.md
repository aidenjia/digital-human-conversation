## 1. Persistence

- [x] 1.1 共享库 DDL：`dimension_voice`、`user_dimension_config`（本仓记录 SQL 于 design/docs；H2/测试 schema 同步）
- [x] 1.2 Entity/Mapper：`DimensionVoice`、`UserDimensionConfig`（ASSIGN_ID；不改 companion 实体）
- [x] 1.3 Redis：`dimension:{userId}` cache store（与陪伴隔离 key；失败不挡主路径）

## 2. Catalog API

- [x] 2.1 `GET /api/dimension-voices`：启用预设升序；`used` 按当前用户配置；无定制、无角色 query
- [x] 2.2 Resource Server：`/api/dimension-voices`、`/api/dimension-voices/**`、`/api/user-dimension-configs`、`/api/user-dimension-configs/**` 设为 `authenticated()`
- [x] 2.3 测试：缺 token / 无效 token / 合法列表；disabled 省略；local 时全部 `used=false`

## 3. User config upsert and echo

- [x] 3.1 `POST /api/user-dimension-configs`：按 `sub` 一行 upsert；音色必选 local 或 `dimension_voice` 的 `voiceBusinessCode`；拒绝 custom
- [x] 3.2 缺省 1/50/100；文案空白存 null；人设 ≤2000；欢迎 ≤100；安抚 ≤50；无 idle 字段
- [x] 3.3 `GET .../audio|interaction|persona`：`configured` 语义；interaction 无 idle
- [x] 3.4 测试：首次插入、再次更新不第二行、未选音色 400、未知/下架音色 400、custom 400、超长人设 400、鉴权失败

## 4. Join linkage

- [x] 4.1 `DimensionJoinPreferenceService`：按 `sub` 解析 TTS/欢迎/安抚；复用 `TencentTtsParamMapper`
- [x] 4.2 `ConvoaiService`：无 `businessCode` 且 `MMD` 走次元；有 `businessCode` 仍只走陪伴；不改陪伴 Service 内部
- [x] 4.3 join 不读写 Redis；人设仅由 upsert 写 `dimension:{sub}`；MUST NOT 写 `companion:{sub}`
- [x] 4.4 测试：MMD 无配置用 yml；有配置覆盖；带 businessCode 不读次元表；idle 仍全局

## 5. Docs and verify

- [x] 5.1 更新 `README.md`、`docs/convoai-client-integration.md`（次元路径、MMD join、跨仓 Redis 约定）
- [x] 5.2 `mvn spotless:apply` 与 `mvn verify`
