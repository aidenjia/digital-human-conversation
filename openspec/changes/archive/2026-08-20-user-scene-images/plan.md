# User Scene Images Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 用户可按陪伴/次元 scope 上传、列表、硬删场景图（COS），并在现有 config upsert 中必选 local 或云端图。

**Architecture:** 新建 `user_scene_image`（`user_id` + `scope`，不绑角色）与 `api.scene` / `scene` / `infrastructure.cos`。陪伴与次元 Service 只解析 `sceneImageId`，不调 COS SDK。选用写入 `scene_source` + `selected_scene_image_id`。Join 不注入场景 URL。

**Tech Stack:** Java 21、Spring Boot 3.5、OAuth2 Resource Server、MyBatis-Plus、腾讯云 COS SDK（固定版本）、JUnit、Mockito、`mvn verify`。

## Global Constraints

- 无 DB 外键；归属/scope 在应用层校验
- `user_id` 只来自 JWT `sub`
- 场景必选：`local` 或 `cloud` + `sceneImageId`；不允许空
- `local` 落库 `scene_source=''`，id 为 null；`local` 禁止同时带 id
- 图库不绑角色；companion / dimension 图库用 `scope` 分开
- 硬删；引用行回退 local；数量不限额
- 密钥走 `CosProperties` + 环境变量；禁止日志输出 secret / token
- 统一信封 `ApiResponse`；新路径 `authenticated()`
- 本仓无 Flyway；Spotless AOSP；行宽 100
- 测试：Service Mockito；鉴权 `@WebMvcTest`；意图写在 `@DisplayName`

## File map

**New**

- `docs/sql/user-scene-images.sql`
- `infrastructure/persistence/entity/UserSceneImage.java`
- `infrastructure/persistence/mapper/UserSceneImageMapper.java`
- `config/CosProperties.java`
- `infrastructure/cos/CosObjectStore.java`（接口）+ `TencentCosObjectStore.java`
- `scene/UserSceneImageService.java`
- `scene/command/*`（upload / list query）
- `api/scene/UserSceneImageController.java`
- `api/scene/dto/*`（item response、upload 无需 JSON body）
- `src/test/.../scene/UserSceneImageServiceTest.java`
- `src/test/.../api/scene/UserSceneImageApiAuthTest.java`

**Edit**

- `pom.xml`：COS SDK
- `UserCompanionConfig.java` / `UserDimensionConfig.java`：场景列
- `UpsertUserCompanionConfigRequest` / `Command` / `UserCompanionConfigService`
- `UpsertUserDimensionConfigRequest` / `Command` / `UserDimensionConfigService`
- `ResourceServerSecurityConfig.java`
- 既有 companion/dimension Service 单测：upsert 补合法场景字段
- `README.md`、`.cursor/rules/java-api-convoai.mdc`
- `application.yml` / `application-local.yml`：`digital-human.cos.*` 占位（无明文密钥）

**Specs:** `openspec/changes/user-scene-images/specs/`

---

## Task 1: Persistence and COS config

- [ ] **Step 1:** 写 `docs/sql/user-scene-images.sql`：`user_scene_image`（id、user_id、scope CHECK、name CHECK trim 长度、object_key、url、content_type、byte_size、metadata jsonb 默认 `{}`、审计列）；索引 `(user_id, scope, created_at DESC)` 与 UNIQUE `(user_id, object_key)`。两张 config：`scene_source text NOT NULL DEFAULT ''`、`selected_scene_image_id text NULL`；CHECK `scene_source IN ('','cloud')` 与 local/cloud 互斥。注释写明无 FK。
- [ ] **Step 2:** Entity `UserSceneImage` 对齐现有 persistence 风格（`ASSIGN_ID`、getter/setter、`FieldFill`）。
- [ ] **Step 3:** `UserCompanionConfig` / `UserDimensionConfig` 增加 `sceneSource`、`selectedSceneImageId`。
- [ ] **Step 4:** `UserSceneImageMapper extends BaseMapper`。
- [ ] **Step 5:** `CosProperties`（`prefix = digital-human.cos`）：region、bucket、secretId、secretKey、signedUrlExpireSeconds。yml 仅非密钥项；密钥文档化为 env。
- [ ] **Step 6:** `pom.xml` 固定版本 COS SDK。`CosObjectStore`：`put(key, bytes, contentType)` 返回可拼 url 所需 key；`delete(key)`。实现类用 SDK；单测 Mock 接口即可。测试 profile 给假 bucket/url，避免启动缺配置（若 Bean 条件化：无配置时测试不创建真实 Client）。
- [ ] **Step 7:** Commit：`Add scene image schema and COS client skeleton`

## Task 2: Scene image APIs (TDD)

- [ ] **Step 1:** 写失败测试 `UserSceneImageApiAuthTest`：无 JWT 的 GET/POST/DELETE → 401。先登记 Security 路径使测试可编译。
- [ ] **Step 2:** `UserSceneImageServiceTest`：缺 name / 非法 scope / 非图片类型 → `ApiException` 400 且不调 `CosObjectStore.put`、不 insert。
- [ ] **Step 3:** 实现 upload：校验 → put COS → insert。失败 COS 不写库。Controller `POST` multipart。
- [ ] **Step 4:** 列表测试：只返回本用户本 scope；cloud 选用 `used=true`；local 全 false；缺 scope 400。实现 GET。
- [ ] **Step 5:** 删除测试：owner 删行并 `delete` COS；引用 config 回退 local；他人 id 不删。实现 DELETE（先清 config 再删行再 COS）。
- [ ] **Step 6:** `ResourceServerSecurityConfig` 加入 `/api/user-scene-images`、`/api/user-scene-images/**`。
- [ ] **Step 7:** 跑 `UserSceneImage*` 测试通过后 Commit：`Add user scene image upload list and delete APIs`

## Task 3: Upsert scene selection (TDD)

- [ ] **Step 1:** 扩展 `UserCompanionConfigServiceTest`：缺场景 400 不写库；`sceneSource=local` 落 `''`+null；合法 `sceneImageId` 写 cloud；他人图 / dimension scope 图 400；`local`+id 400。既有音色用例补 `sceneSource=local`（或合法 id）。
- [ ] **Step 2:** Request/Command 加字段。Service 注入 `UserSceneImageMapper`（或 Scene 解析组件）校验归属。
- [ ] **Step 3:** 同样改次元 upsert 测试与 Service；companion scope 图在次元 upsert 必须 400。
- [ ] **Step 4:** 跑 companion + dimension 相关测试。
- [ ] **Step 5:** Commit：`Require scene selection on companion and dimension upsert`

## Task 4: Docs and verify

- [ ] **Step 1:** `README.md`：新三接口；upsert 必选场景；受保护路径；COS env（不写密钥值）。
- [ ] **Step 2:** `.cursor/rules/java-api-convoai.mdc` 契约表同步。
- [ ] **Step 3:** `mvn spotless:apply` && `mvn verify`
- [ ] **Step 4:** Commit：`Document user scene image APIs`

## Self-review

- 规格场景均有对应测试意图（缺 token、缺场景、跨用户、跨 scope、删除回退）
- 无 Flyway、无外键、join 不读场景
- 旧 upsert 客户端破坏性 400 已在 README 标明
