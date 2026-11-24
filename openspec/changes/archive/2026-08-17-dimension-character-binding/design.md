## Context

`digital-human-conversation` 是 ConvoAI HTTP 网关（CD 2 副本、无粘滞会话），与 agent 共用 Postgres / Redis。次元配置已上线：`dimension_voice` + 按 `user_id` 一行的 `user_dimension_config`（人设/音色/微调/互动），无角色目录；`GET /persona` 按用户回显。

对照原型后纠正：人设与音色跟次元**角色**走；微调/互动不跟角色。一个用户同时只有一份配置。陪伴约定「不使用 DB 外键」继续遵守。

本仓无 Flyway；新表/新列由共享库 DDL 与本 change 同步。

## Goals / Non-Goals

**Goals:**

- 新建 `dimension_character` 目录与 `GET /api/dimension-characters`（`businessCode`、`name`、`used`、`personaText`）
- `user_dimension_config.dimension_character_id` 逻辑引用（可空、无 FK）
- POST 必带角色 `businessCode`；删除 GET persona
- 音色列表必带角色 `businessCode` 才能标 `used`
- audio / interaction 行为不变；MMD join 仍按 `sub` 读一行
- 受保护路径加上 `/api/dimension-characters`

**Non-Goals:**

- `(user × 角色)` 多行历史、DB 外键、定制音色、idle、改陪伴表/API
- 角色目录上的可写人设 API（人设由运营维护 `dimension_character.persona_text`）
- 本仓 Flyway；agent 仓改读 key 逻辑（仍写 `dimension:{sub}`）

## Decisions

### 1. 每用户一行 + 逻辑角色 id，不要外键

唯一键仍是 `user_id`。列 `dimension_character_id` 存 `dimension_character.id`，**不建 FOREIGN KEY**。POST 时查启用角色：`business_code` 命中 0 条或未启用 → 400；唯一性由目录 `UNIQUE(business_code)` + 应用层「必须恰好一条启用」保证。

换角色 = UPDATE 同一行（覆盖音色；人设随新角色目录；微调/互动留在这行）。

相对「两张配置表」：用户明确同时只有一份，分表 YAGNI。相对「不建目录」：列表 `name` 与 code 校验需要目录。

### 2. 人设回显走角色列表，不走 GET persona

`GET /api/dimension-characters` 对标 `GET /api/companions`：`personaText` 来自角色目录 `dimension_character.persona_text`，每项都回显。删除 `GET /api/user-dimension-configs/persona`（破坏性，客户端改拉角色列表）。POST MUST NOT 收人设。

### 3. 音色 `used` 必须带角色 query

`GET /api/dimension-voices?businessCode=` 必填。列表条目仍是全局启用预设（不按角色裁剪）。`used=true` 当且仅当：query 角色 = 当前行角色，且 `voice_source=preset` 且 `voice_id` 对应该项。缺 query / 未知 / 未启用 → 400。

### 4. HTTP 与包

仍在 `api.dimension` / `dimension`。新路径 `authenticated()`。POST body 的 `businessCode` = 次元角色（来自角色列表），与 `voiceBusinessCode` 勿混；MUST NOT 用陪伴 `UE5_*` 当次元角色（目录校验会 400）。

### 5. join / Redis

不改分流条件：`MMD` 且无陪伴 `businessCode` → 读这一行 TTS/欢迎/安抚。join 不读角色目录。upsert 写 `dimension:{sub}.instruction` = 所选角色目录 `persona_text` 的 trim 值。

### 6. 旧数据

已有配置行 `dimension_character_id` 为 null：GET audio/interaction 仍 `configured=true`；角色列表全部 `used=false`，但每项仍带回目录人设；音色列表在合法 `businessCode` 下全部 `used=false`。下次带角色的 POST 补上 id。

## Risks / Trade-offs

- [Risk] 共享库未加表/列导致启动或 IT 失败 → Mitigation：tasks 含 DDL；文档 SQL；测试 schema 同步
- [Risk] 客户端仍调 GET persona → Mitigation：spec/README 标明删除；切片测试 404 或无 mapping
- [Risk] 换角色后 Redis 人设切到新角色目录文案 → Mitigation：产品接受；文档写明人设跟目录
- [Risk] 旧行无人设挂点 → Mitigation：客户端必须再 POST 一次带 `businessCode`

## Migration Plan

1. 共享 Postgres：建 `dimension_character`（含 UNIQUE `business_code`）、`user_dimension_config` 加可空 `dimension_character_id`（无 FK）；种子启用角色
2. 发 conversation（新列表、POST 校验、删 persona、voices query）
3. 客户端改对接：角色列表取人设；voices 带 `businessCode`；POST 带角色 code
4. Rollback：停用新路径/回退镜像；列可留；陪伴不受影响

## Open Questions

- 无。
