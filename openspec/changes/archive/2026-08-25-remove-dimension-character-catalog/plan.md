# Remove Dimension Character Catalog — Implementation Plan

**Goal:** 移除全局次元角色目录；人设与角色身份迁回 `user_dimension_config`；删除 `GET /api/dimension-characters`；简化 `GET /api/dimension-voices`。

**Architecture:** 删 `DimensionCharacter*` 与 catalog 列表端点；`UserDimensionConfig` 增三列；`UserDimensionConfigService.upsert` 直写用户行 + Redis；`DimensionCatalogService.listVoices` 仅读用户行算 `used`。

**Tech Stack:** Java 21、Spring Boot 3.5、MyBatis-Plus、JUnit、`mvn verify`。

## Global Constraints

- 不改陪伴 API / 表
- `user_id` 仅 JWT `sub`；统一 `ApiResponse`
- 本仓无 Flyway；DDL 文档在 `docs/sql/`
- Spotless AOSP；行宽 100

## File map

**Delete**

- `DimensionCharacter.java`、`DimensionCharacterMapper.java`
- `DimensionCharacterItemResponse.java`
- `GET /api/dimension-characters` handler

**Edit**

- `UserDimensionConfig.java`：三角色字段；移除 `dimensionCharacterId`、场景列
- `UserDimensionConfigService.java`：upsert 写三字段 + Redis；移除 `SceneSelectionResolver`（场景字段从 POST 删除）
- `UpsertUserDimensionConfigRequest` / `Command`
- `DimensionCatalogController`：仅 `GET /dimension-voices`
- `DimensionCatalogService.java`：`listVoices` 简化
- `UserSceneImageService.java`：次元 scope 不再读 config 场景列
- `ResourceServerSecurityConfig`（若曾单独登记 characters 路径则清理）
- 测试：`DimensionApiAuthTest`、`DimensionCatalogServiceTest`、`UserDimensionConfigServiceTest`
- 活规格、`README.md`、`java-api-convoai.mdc`；删 `openspec/specs/dimension-character-catalog/`

**DDL**

- `docs/sql/drop-dimension-character.sql`

## Steps

- [x] 1. 编写并评审 DDL（回填 → 删 `dimension_character_id` → 删表）
- [x] 2. 删实体/Mapper/DTO 与 `GET /dimension-characters`
- [x] 3. upsert 写用户行三字段；删 `GET /persona`（若曾存在）
- [x] 4. 简化 `listVoices`；更新活规格与 README
- [x] 5. 测试 + `mvn verify`；Apifox 同步（可选）
