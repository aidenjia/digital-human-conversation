## ADDED Requirements

### Requirement: MMD join MUST apply dimension preferences by userId

当 `POST /api/convoai/agents` 的 `digitalHumanType` 为 `MMD` 时，系统 MUST 按 JWT `sub` 加载启用的 `user_dimension_config`（若存在），将映射后的腾讯 TTS 参数覆盖本请求，并按配置写入欢迎语与安抚（`silence_config`）。`idle_timeout` MUST 始终取自 `convoai.idle-timeout`（默认 300 秒），MUST NOT 从次元配置读取 idle。请求体 MUST NOT 要求角色 `businessCode`。

当无用户配置行时，系统 MUST 使用 yml TTS / 欢迎语路径，MUST NOT 因次元配置表缺失而失败。当配置为本地音色或无法解析云端 `voice_type` 时，TTS `voice_type` MUST 回落 yml 默认音色。当欢迎语/安抚文案为 null 或空白时，MUST NOT 向用户配置或上游补写官方默认话术。

当 `digitalHumanType` 为 `UE5` 时，系统 MUST 走陪伴 join 偏好路径，MUST NOT 读取 `user_dimension_config`。当 `digitalHumanType` 为 `Unity` 时，系统 MUST 使用 yml 默认，MUST NOT 读取 `user_companion_config` 或 `user_dimension_config`。

#### Scenario: MMD join without config keeps yml defaults

- **WHEN** 客户端 join 且 `digitalHumanType=MMD`、该用户无次元配置
- **THEN** 系统 MUST 使用配置默认 TTS 与 yml 欢迎语路径且 MUST 调用上游创建 agent

#### Scenario: MMD join with saved config applies overrides

- **WHEN** 客户端 join 且 `digitalHumanType=MMD`、该用户已有启用次元配置
- **THEN** 上游请求 MUST 使用配置映射后的 TTS（本地则 yml `voice_type`），并按开关与非空文案写入欢迎语/安抚

#### Scenario: Companion join ignores dimension config

- **WHEN** 客户端 join 且 `digitalHumanType` 为 `UE5`
- **THEN** 系统 MUST NOT 读取 `user_dimension_config` 注入本请求

#### Scenario: Unity join uses yml without querying preference tables

- **WHEN** 客户端 join 且 `digitalHumanType` 为 `Unity`
- **THEN** 系统 MUST 使用 yml TTS / 欢迎语路径，MUST NOT 查询 `user_companion_config` 或 `user_dimension_config`

#### Scenario: Join idle_timeout always uses global config

- **WHEN** 客户端以 MMD join
- **THEN** 上游 `idle_timeout` MUST 等于 `convoai.idle-timeout`

---

### Requirement: Dimension join MUST NOT touch Redis

当 MMD join 时，系统 MUST NOT 读写 Redis。人设缓存仅由次元 upsert 维护。

#### Scenario: MMD join does not touch Redis

- **WHEN** 客户端以 MMD join（无论是否有次元配置）
- **THEN** 系统 MUST NOT 写入或读取 `dimension:{sub}` / `companion:{sub}`

---

### Requirement: Tencent TTS scale mapping MUST be deterministic

系统 MUST 将库内量纲映射为腾讯 TTS：`speech_rate`→`speed`（1.0→0）；`volume` 0–100→（50→0）；`pitch`（100→0）。相同输入 MUST 得到相同输出。系统 MUST 复用现有 `TencentTtsParamMapper`，MUST NOT 复制一套不同公式。

#### Scenario: Neutral volume maps to zero

- **WHEN** 次元配置 `volume` 为 50
- **THEN** 映射后的腾讯 `volume` MUST 为 0
