## ADDED Requirements

### Requirement: Join MUST apply companion preferences by userId

`POST /api/convoai/agents` 请求体 MUST NOT 要求角色 `businessCode`。当 `digitalHumanType` 为 `UE5` 时，系统 MUST 按 JWT `sub` 加载该用户最近一次启用的 `user_companion_config`（`updated_at` 最新，与角色列表 `used` 同源）。若存在配置，系统 MUST 将映射后的腾讯 TTS 参数覆盖本请求，并按配置写入欢迎语与安抚（`silence_config`）。当 `digitalHumanType` 为 `Unity` 时，系统 MUST NOT 查询 `user_companion_config`，MUST 使用 yml TTS / 欢迎语路径。`idle_timeout` MUST 始终取自 `convoai.idle-timeout`（默认 **300** 秒）。

当无用户配置行时，系统 MUST 使用 yml TTS / 欢迎语路径，MUST NOT 因用户配置表缺失而失败。当配置行为本地音色（`voice_source` 空串）或无法解析云端 `voice_type` 时，TTS `voice_type` MUST 回落服务端 yml 默认音色，MUST NOT 注入已删除的默认链 `gentle_female`。当配置存在但欢迎语/安抚文案为 null 或空白时，MUST NOT 因「官方默认话术」向用户配置或上游补写文案。

#### Scenario: Join without saved config keeps yml defaults

- **WHEN** 客户端 join 且 `digitalHumanType` 为陪伴类型、该用户无启用陪伴配置
- **THEN** 系统 MUST 使用配置默认 TTS 与 yml 欢迎语路径，MUST NOT 因用户配置表缺失而失败

#### Scenario: Join with saved config applies latest overrides

- **WHEN** 客户端 join 且该用户已有启用陪伴配置
- **THEN** 上游请求 MUST 使用最近一次启动保存映射后的 TTS（本地则 yml `voice_type`），并按开关与非空文案写入欢迎语/安抚

#### Scenario: Join idle_timeout always uses global config

- **WHEN** 客户端 join
- **THEN** 上游 `idle_timeout` MUST 等于 `convoai.idle-timeout`

#### Scenario: Unity join uses yml without querying companion config

- **WHEN** 客户端 join 且 `digitalHumanType` 为 `Unity`
- **THEN** 系统 MUST 使用 yml TTS / 欢迎语路径，MUST NOT 查询 `user_companion_config`

---

### Requirement: Tencent TTS scale mapping MUST be deterministic

系统 MUST 将库内量纲映射为腾讯 TTS：`speech_rate`→`speed`（1.0→0）；`volume` 0–100→（50→0）；`pitch`（100→0）。相同输入 MUST 得到相同输出。

#### Scenario: Neutral volume maps to zero

- **WHEN** `volume` 为 50
- **THEN** 映射后的腾讯 `volume` MUST 为 0
