## 1. Persistence and COS config

- [x] 1.1 DDL 文档：新建 `user_scene_image`（无 `companion_id`；必填 `name`/`url`/`object_key`/`scope`）；`user_companion_config` 与 `user_dimension_config` 增加 `scene_source`（默认 `''`）与可空 `selected_scene_image_id`，含 CHECK（无 FK）
- [x] 1.2 Entity/Mapper：`UserSceneImage`；两张 config 实体增加场景列
- [x] 1.3 `CosProperties` + 环境变量绑定（bucket/region/secret）；签名 URL TTL 与大小上限写在 `ScenePreferenceDefaults`；密钥不进 Git
- [x] 1.4 `pom.xml` 增加腾讯云 COS SDK（固定版本，禁止 LATEST）；`infrastructure.cos` Client 封装 put/delete

## 2. Scene image APIs

- [x] 2.1 `POST /api/user-scene-images`：multipart `file`+`name`+`scope`；代理上传 COS 后写库；返回 `id`/`name`/`url`
- [x] 2.2 `GET /api/user-scene-images?scope=`：当前用户该 scope 列表；`used` 对照使用中配置
- [x] 2.3 `DELETE /api/user-scene-images/{id}`：硬删；引用该图的 config 回退 local；越权拒绝
- [x] 2.4 Resource Server 登记 `/api/user-scene-images`/`**` 为 `authenticated()`
- [x] 2.5 测试：缺 token；非法 scope/name/类型；列表隔离用户；local 时 used 全 false；删除回退选用；他人 id 拒绝

## 3. Companion / dimension upsert scene selection

- [x] 3.1 Request/Command 增加 `sceneSource`、`sceneImageId`；校验对齐音色（local 或 cloud id，不允许空；local 禁止带 id）
- [x] 3.2 Service：校验图归属 `user_id` + 对应 scope；落库 `scene_source` / `selected_scene_image_id`
- [x] 3.3 测试：缺场景 400；local 落空串；cloud 写 id；跨用户/跨 scope 400；既有音色用例仍带合法场景字段

## 4. Docs and verify

- [x] 4.1 更新 `README.md`、`.cursor/rules/java-api-convoai.mdc`（新路径、upsert 破坏性必选场景、COS 配置）
- [x] 4.2 `mvn spotless:apply` 与 `mvn verify`
