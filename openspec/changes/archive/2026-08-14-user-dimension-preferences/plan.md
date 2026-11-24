# User Dimension Preferences Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 交付次元数字人独立音色目录、按 userId 的偏好 upsert/回显，以及 MMD join 注入且不改陪伴契约。

**Architecture:** `api.dimension` 薄 Controller；`dimension` 编排；新表 Entity/Mapper；`ConvoaiService` 仅增加「无 businessCode + MMD」分支。Redis key `dimension:{sub}` 与 `companion:{sub}` 隔离。

**Tech Stack:** Java 21、Spring Boot 3.5、OAuth2 Resource Server、MyBatis-Plus、现有 `TencentTtsParamMapper`、JUnit、`mvn verify`。

## Global Constraints

- 不改 `companion` 表与 `/api/companions*`、`/api/user-companion-configs*` 行为
- 不建角色表、不接定制音色、互动无 idle 字段
- `user_id` 只来自 JWT `sub`；新路径 `authenticated()`
- 统一信封 `ApiResponse`；成功 upsert 无 data 摘要
- 量纲复用 `TencentTtsParamMapper`（1→speed 0；volume 50→0；pitch 100→0）
- 本仓无 Flyway；DDL 先落到共享库，测试用 H2 schema
- Spotless Google Java Format AOSP；行宽 100

## File map

**New**

- `infrastructure/persistence/entity/DimensionVoice.java`
- `infrastructure/persistence/entity/UserDimensionConfig.java`
- `infrastructure/persistence/mapper/DimensionVoiceMapper.java`
- `infrastructure/persistence/mapper/UserDimensionConfigMapper.java`
- `dimension/*` Service、command、Redis adapter（可仿 `companion` 但不 import 其 Service）
- `api/dimension/*Controller` 与 `api/dimension/dto/*`
- `src/test/resources` H2 建表语句（若现有 test schema 需追加）
- `docs/` 片段：DDL 供 DBA（可放 `docs/sql/dimension-tables.sql`，仅 SQL 无密钥）

**Edit**

- `ResourceServerSecurityConfig.java` 增加路径
- `ConvoaiService.java` 分流（有 `businessCode` 仍只调陪伴）
- `README.md`、`docs/convoai-client-integration.md`
- 测试：`ConvoaiServiceTest` 增加 MMD 用例；新 `*AuthTest`

---

## Task 1: Persistence

- [ ] **Step 1:** 编写 `docs/sql/dimension-tables.sql`：`dimension_voice`（id PK、business_code UNIQUE、name、description、tecent_voice_type_id、enabled、审计列）；`user_dimension_config`（id PK、user_id UNIQUE、voice_id 可空、voice_source CHECK preset 或后续以空串存 local——实现时与陪伴一样应用层空串、库约束需允许 `''` 与 `preset`、greeting/comfort/persona 可空、微调 CHECK 对齐陪伴、无 custom_voice_id、无 idle）
- [ ] **Step 2:** 测试 H2 执行该 DDL（或 `schema.sql` include）
- [ ] **Step 3:** 生成/手写 Entity+Mapper，字段与表一致
- [ ] **Step 4:** Redis store：key `dimension:` + userId；序列化含 `instruction`；单测 key 不与 `companion:` 碰撞
- [ ] **Step 5:** Commit

## Task 2: Catalog API

- [ ] **Step 1:** 写失败测试：无 JWT → 401
- [ ] **Step 2:** 实现 `GET /api/dimension-voices` + Service（enabled 升序、used）
- [ ] **Step 3:** Security 登记路径
- [ ] **Step 4:** 成功列表测试：disabled 省略；preset used；local 全 false
- [ ] **Step 5:** Commit

## Task 3: Upsert and echo

- [ ] **Step 1:** 写失败测试：未选音色、custom、未知/下架 voice、人设 >2000、无 JWT
- [ ] **Step 2:** 实现 POST upsert（local 空串；文案 null；1/50/100；刷 Redis）
- [ ] **Step 3:** GET audio / interaction / persona（`configured`；interaction 无 idle 字段）
- [ ] **Step 4:** 测试首次 INSERT、再次 UPDATE 仍一行
- [ ] **Step 5:** Commit

## Task 4: Join linkage

- [ ] **Step 1:** 写测试：MMD 无配置 → yml TTS；MMD 有配置 → 覆盖；同请求带 businessCode → 不读次元表（mock 次元 service never）；idle 仍 `props.getIdleTimeout()`
- [ ] **Step 2:** 实现 `DimensionJoinPreferenceService` + `ConvoaiService` 分流
- [ ] **Step 3:** 断言 Redis 只写 `dimension:` 不写 `companion:`
- [ ] **Step 4:** Commit

## Task 5: Docs and verify

- [ ] **Step 1:** README + `docs/convoai-client-integration.md`：次元接口表、MMD join 不要带陪伴 businessCode、agent 需读 `dimension:{sub}`
- [ ] **Step 2:** `mvn spotless:apply` && `mvn verify`
- [ ] **Step 3:** Commit
