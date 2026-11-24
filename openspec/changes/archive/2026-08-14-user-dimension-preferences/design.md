## Context

`digital-human-conversation` 已是 ConvoAI HTTP 网关（2 副本、无粘滞会话），与 agent 共用 Postgres / Redis。陪伴配置（`companion*` 表与 `/api/companions*`）已上线：角色目录 + 用户×角色偏好 + join 可选 `businessCode`。agent 从 Redis `digital-human:companion:{sub}` 读取 `instruction` 作为人设。

Jupiter SR 次元数字人需要声音（预设音色 + 微调）、互动（欢迎语/安抚、不含静默退出）、人设（绑登录用户）。形象是客户端本地 MMD，本服务不建角色目录。

本仓无 Flyway；新表由共享库 DDL 先行或与本 change 同步上线。

## Goals / Non-Goals

**Goals:**

- 独立预设音色目录 `dimension_voice` 与 `GET /api/dimension-voices`
- 按 JWT `sub` 一行的 `user_dimension_config`：人设、音色、微调、欢迎语/安抚
- 回显 GET audio / interaction / persona；启动 POST upsert
- join：`digitalHumanType=MMD` 且无 `businessCode` 时注入次元 TTS/欢迎/安抚；Redis `dimension:{sub}` 写入人设
- 陪伴表、陪伴 HTTP、陪伴 Redis key 行为不变

**Non-Goals:**

- 次元角色目录、定制音色、试听、订阅、形象/声音定制工单
- 静默退出可配（idle 仍全局 yml）
- 本仓引入 Flyway
- 实现 digital-human-agent 的 key 选择（须另 change；本仓文档约定缓存 JSON 字段）
- 改 Users 发 token

## Decisions

### 1. 英文名 `dimension`

对应产品「次元」。URL / 表 / Java 包同一前缀。不使用 `product_line` 污染 `companion`。

### 2. 两张新表，零改陪伴表

`dimension_voice`：预设目录（无 `requires_subscription`）。`user_dimension_config`：`user_id` UNIQUE；`voice_id` FK 逻辑指向 `dimension_voice`；`voice_source` 为 `preset` 或落库空串（local）；无 `custom_voice_id`。

相对陪伴：粒度是用户而非用户×角色；多 `persona_text`；少定制声。

### 3. HTTP 完全隔离

新 Controller 在 `api.dimension`。Resource Server 增加 `/api/dimension-voices`、`/api/user-dimension-configs/**` 为 `authenticated()`。不扩展陪伴路径。

### 4. join 分流（`trtc`，不进 `companion` 包）

- 非空 `businessCode` → 现有 `CompanionJoinPreferenceService`（不变）
- 无 `businessCode` 且 `digitalHumanType=MMD` → `DimensionJoinPreferenceService`
- 其余 → 仅 yml（现行为）

禁止在陪伴 Service 内 `if (MMD)`。TTS 量纲复用 `TencentTtsParamMapper`（已在 `companion.support`；次元可调用该 mapper，或抽到 `trtc.support`——若抽，须小 diff 且单测搬家；推荐次元直接依赖现有 mapper 类，这是共享工具不是陪伴用例 API）。

若认为跨包引用 `companion.support.TencentTtsParamMapper` 仍算耦合：本期允许这一处共享映射，不复制公式。

### 5. Redis

逻辑 key `dimension:{userId}`，NameMapper 前缀仍 `digital-human:`。JSON 与陪伴 cache 同构足够 agent 读取 `instruction`（= trim 后的 `persona_text`；空则字段省略或空串，agent 回落默认人设）。TTL 与陪伴一致（1 天）。2 副本下 join 读 cache 未命中则回源 DB。

### 6. 校验与缺省

与陪伴对齐：语速 1 / 音量 50 / 音高 100；欢迎/安抚开关默认 true，文案空白存 null；字数 100 / 50；人设 ≤2000；音色必选。

## Risks / Trade-offs

- [Risk] 共享库未建表导致启动/IT 失败 → Mitigation：tasks 含 DDL；单测 H2 建表或 schema.sql
- [Risk] agent 仍只读 companion key → Mitigation：次元人设暂不生效但不冲陪伴；文档 + 跨仓 follow-up
- [Risk] 客户端 join 误带陪伴 `businessCode` → Mitigation：spec 写明 MMD 不要带；带了则走陪伴路径（可能 400 unknown companion）
- [Risk] 两份音色种子运维成本 → Mitigation：有意为之，产品可独立上下架

## Migration Plan

1. 共享 Postgres 执行 DDL 与可选种子 `dimension_voice`
2. 发 conversation（新 API + join 分支）
3. 客户端对接次元路径后再切 MMD join
4. Rollback：停用新路径即可；陪伴不受影响；表可保留

## Open Questions

- 无。
