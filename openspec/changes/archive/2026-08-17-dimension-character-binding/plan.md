# Dimension Character Binding Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 次元配置绑定当前角色：角色目录列表回显人设，音色 `used` 按角色，POST 必带角色 code；删除独立 GET persona。

**Architecture:** 新建 `dimension_character` 目录；`user_dimension_config` 增加逻辑列 `dimension_character_id`（无 FK）。Catalog 仍走 `DimensionCatalogController` / `DimensionCatalogService`；upsert 仍走 `UserDimensionConfigService`（每用户一行）。join / Redis 路径不改。

**Tech Stack:** Java 21、Spring Boot 3.5、OAuth2 Resource Server、MyBatis-Plus、JUnit、`mvn verify`。

## Global Constraints

- 不改 `companion*` 表与 `/api/companions*`、`/api/user-companion-configs*`
- **禁止 DB 外键**；角色存在性/启用/`business_code` 唯一性在应用层校验
- 每用户一行（`user_id` UNIQUE）；换角色覆盖人设/音色
- `user_id` 只来自 JWT `sub`；新路径 `authenticated()`
- 统一信封 `ApiResponse`；成功 upsert 无 data 摘要
- 无定制音色、无 idle；本仓无 Flyway
- Spotless Google Java Format AOSP；行宽 100

## File map

**New**

- `infrastructure/persistence/entity/DimensionCharacter.java`
- `infrastructure/persistence/mapper/DimensionCharacterMapper.java`
- `api/dimension/dto/DimensionCharacterItemResponse.java`

**Edit**

- `UserDimensionConfig.java`：加 `dimensionCharacterId`
- `DimensionCatalogController.java`：`GET /dimension-characters`；`listVoices(businessCode)`
- `DimensionCatalogService.java`：角色列表 + voices 必填角色
- `UserDimensionConfigController.java`：删除 GET persona
- `UpsertUserDimensionConfigRequest` / `Command`：加角色 `businessCode`
- `UserDimensionConfigService.java`：解析角色 id；删除 `findPersonaConfig`
- 删除 `UserDimensionPersonaConfigResponse.java`
- `ResourceServerSecurityConfig.java`：登记 `/api/dimension-characters`**
- 测试：`DimensionApiAuthTest`、`DimensionCatalogServiceTest`、`UserDimensionConfigServiceTest`
- `README.md`、`.cursor/rules/java-api-convoai.mdc`
- DDL 说明（新建或更新 `docs/sql/dimension-tables.sql` / 等同 schema 文档）

---

## Task 1: Persistence

- [ ] **Step 1:** 写 DDL：`dimension_character`（`id` PK、`business_code` UNIQUE NOT NULL、`name` NOT NULL、`enabled` NOT NULL default true、审计列）；`ALTER TABLE user_dimension_config ADD dimension_character_id text NULL`。注释写明无 FK。
- [ ] **Step 2:** Entity `DimensionCharacter` 对齐 `DimensionVoice` 风格（ASSIGN_ID、`businessCode`/`name`/`enabled`）。
- [ ] **Step 3:** `UserDimensionConfig` 增加 `dimensionCharacterId` getter/setter；类注释改为「人设/音色跟当前角色，微调/互动跟用户；无 FK」。
- [ ] **Step 4:** `DimensionCharacterMapper` extends BaseMapper。
- [ ] **Step 5:** Commit

## Task 2: Character catalog API

- [ ] **Step 1:** 写失败测试：`GET /api/dimension-characters` 无 JWT → 401（扩 `DimensionApiAuthTest`）。
- [ ] **Step 2:** DTO `DimensionCharacterItemResponse(businessCode, name, used, personaText)`。
- [ ] **Step 3:** `DimensionCatalogService.listCharacters()`：启用角色升序；`used`/`personaText` 仅当配置 `dimensionCharacterId` 命中。
- [ ] **Step 4:** Controller `GET /dimension-characters`；Security 增加路径。
- [ ] **Step 5:** 单测：未绑定全部 unused+persona null；绑定后 used 一项且带 personaText；disabled 不出现。
- [ ] **Step 6:** 跑相关测试通过后 Commit

## Task 3: Voices used by character

- [ ] **Step 1:** 改 `DimensionCatalogServiceTest`：`listVoices()` 无参改为必填 `businessCode`；缺/未知 400；其他角色 used 全 false；匹配角色+preset used true。
- [ ] **Step 2:** 实现 `listVoices(String businessCode)`：`requireEnabledCharacter`；`used` 还要求 `config.dimensionCharacterId` 等于该角色 id。
- [ ] **Step 3:** Controller `@RequestParam String businessCode`（必填）。
- [ ] **Step 4:** Auth 测试带 query；无 JWT 仍 401。
- [ ] **Step 5:** Commit

## Task 4: Upsert and remove persona GET

- [ ] **Step 1:** Request/Command 增加角色 `businessCode`（`@NotBlank`）；与 `voiceBusinessCode` 分列。
- [ ] **Step 2:** 单测：缺角色 / 未知角色 400 且不写库；成功 insert 写入 `dimensionCharacterId`；第二次 update 同一行。
- [ ] **Step 3:** Service：`catalogService.requireEnabledCharacter` 后 `setDimensionCharacterId`；保留音色/人设/Redis 逻辑。
- [ ] **Step 4:** 删除 GET `/persona`、`UserDimensionPersonaConfigResponse`、`findPersonaConfig`；Auth 测试改为断言无 mapping（404）或不再调用。
- [ ] **Step 5:** Commit

## Task 5: Docs and verify

- [ ] **Step 1:** `README.md`：加角色列表；voices 必带 `businessCode`；POST 必带角色 code；删 persona 行；受保护路径加 `dimension-characters`。
- [ ] **Step 2:** `.cursor/rules/java-api-convoai.mdc` 同步（本仓无独立 client-integration 文档，对外契约以 README 为准）。
- [ ] **Step 3:** `mvn spotless:apply` && `mvn verify`
- [ ] **Step 4:** Commit
