# Tasks: remove-dimension-character-catalog

- [x] 1.1 DDL：`docs/sql/drop-dimension-character.sql`（回填 + 删表/列）
- [x] 1.2 删 `DimensionCharacter*`；`UserDimensionConfig` 三字段；upsert 写用户行 + Redis
- [x] 1.3 删 `GET /dimension-characters`；删 `GET /user-dimension-configs/persona`；简化 `listVoices`
- [x] 1.4 更新活规格、`README.md`、`java-api-convoai.mdc`；删 `dimension-character-catalog`
- [x] 1.5 测试与 `mvn verify`
