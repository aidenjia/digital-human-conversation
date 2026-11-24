# Apply Receipt

- **Change:** remove-dimension-character-catalog
- **Iteration:** 1
- **Timestamp:** 2026-08-25T10:04:00+08:00
- **Branch:** feature/dimension-characters
- **Worktree:** repo root (in-place apply)
- **Executor:** parent agent
- **Tasks completed:** 5 of 5
- **Remaining tasks:** none
- **Notes:** Removed DimensionCharacter* and GET /dimension-characters; UserDimensionConfig stores character_business_code/name/persona_text; dropped scene columns from dimension POST/entity; simplified listVoices; DDL drop-dimension-character.sql executed on shared Postgres; mvn verify passed.
