## 1. Persistence

- [x] 1.1 共享库 DDL 文档：新建 `dimension_character`（id PK、business_code UNIQUE、name、persona_text、enabled、审计列）；`user_dimension_config` 增加可空 `dimension_character_id`（逻辑引用，**无 FK**），并移除 `persona_text`
- [x] 1.2 Entity/Mapper：`DimensionCharacter`（含 `personaText`）；`UserDimensionConfig` 增加 `dimensionCharacterId`、去掉 `personaText`（不改 companion 实体）
- [x] 1.3 测试用 schema / 文档 SQL 与实库列对齐（本仓无 Flyway）

## 2. Character catalog API

- [x] 2.1 `GET /api/dimension-characters`：启用角色升序；每项 `businessCode`、`name`、`used`、`personaText`（人设来自角色目录，每项都有）
- [x] 2.2 Resource Server：`/api/dimension-characters`、`/api/dimension-characters/**` 设为 `authenticated()`
- [x] 2.3 测试：缺 token / 无效 token / 合法列表；disabled 省略；未绑定全部 used=false 且仍带回目录人设；绑定后 used 至多一项

## 3. Voices used by character

- [x] 3.1 `GET /api/dimension-voices`：Query `businessCode` 必填；校验启用且唯一；条目仍全局预设；`used` 仅当前绑定角色+预设音色
- [x] 3.2 测试：缺 query 400；未知/未启用角色 400；其他角色全部 used=false；匹配角色+preset used=true；local 全 false

## 4. Upsert and remove persona GET

- [x] 4.1 `POST /api/user-dimension-configs`：必填角色 `businessCode`；解析 `dimension_character_id` 后仍按 `user_id` upsert 一行；换角色覆盖音色；人设取自所选角色目录并写入 Redis
- [x] 4.2 删除 `GET /api/user-dimension-configs/persona` 及 DTO/Service 方法
- [x] 4.3 GET audio / interaction 保持无 Query、`configured` 语义不变
- [x] 4.4 测试：缺角色 400、未知/下架角色 400、首次插入含 character id、再次更新不第二行、persona GET 无 mapping；既有音色/custom 校验仍失败；POST 不校验收人设

## 5. Docs and verify

- [x] 5.1 更新 `README.md`、`.cursor/rules/java-api-convoai.mdc`（角色列表、删 persona、voices query、POST 角色 code）
- [x] 5.2 `mvn spotless:apply` 与 `mvn verify`
