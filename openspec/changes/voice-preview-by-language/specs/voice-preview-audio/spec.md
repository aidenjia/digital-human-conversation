## RENAMED Requirements

- FROM: `### Requirement: Fixed preview sample text from configuration`
- TO: `### Requirement: Preview sample text and PrimaryLanguage follow catalog language`

## MODIFIED Requirements

### Requirement: Preview voice audio by businessCode

系统 SHALL 提供 `GET /api/voice-previews`（JWT Bearer）。Query MUST 含非空 `businessCode`、`productScope`（`companion`|`dimension`）、`voiceKind`（`preset`|`custom`）。系统 MUST 先按 scope/kind 在对应音色目录表用 `business_code` 定位行并得到雪花 `id`（及腾讯 VoiceType）；预设 MUST 同时读取目录 `language` 列。再以 `(product_scope, voice_kind, voice_ref_id=id)` 查询 `voice_preview_audio`。命中且行内 `sample_text` 等于当前语言目标文案时 MUST 返回库内 PCM，MUST NOT 调用腾讯合成。未命中、或命中但 `sample_text` 与目标文案不一致时 MUST 按当前语言调用腾讯 TextToVoice（`Codec=pcm`，采样率取配置默认 16000，`PrimaryLanguage` 按语言），将 PCM 写入 `voice_preview_audio`（含匹配的 `sample_text`：无行则 INSERT，已有行则 UPDATE），再返回。成功时 `ApiResponse.data` MUST 含 `audioBase64`（PCM 的 base64）、`sampleRate`、`codec`（`pcm`）。系统 MUST NOT 将 PCM 写入 COS，MUST NOT 把 PCM 嵌入 voices 列表响应。系统 MUST NOT 要求客户端传 `language`。

#### Scenario: Cache hit returns stored PCM

- **WHEN** 合法 JWT 请求已知预设 `businessCode` 且试听表已有对应行且 `sample_text` 等于当前语言目标文案
- **THEN** 系统 MUST 返回 200 与 `audioBase64`/`sampleRate`/`codec`，且 MUST NOT 调用 TextToVoice

#### Scenario: Cache miss synthesizes and stores

- **WHEN** 合法 JWT 请求已知预设 `businessCode` 且试听表无对应行
- **THEN** 系统 MUST 调用 TextToVoice，INSERT 试听行，并返回 PCM 载荷

#### Scenario: Stale cache resynthesizes when sample text does not match language

- **WHEN** 合法 JWT 请求已知预设 `businessCode` 且试听表已有对应行，但 `sample_text` 不等于当前语言目标文案
- **THEN** 系统 MUST 调用 TextToVoice，UPDATE 该行 PCM 与 `sample_text`，并返回新 PCM
- **AND** MUST NOT 将旧 PCM 当作有效缓存返回

#### Scenario: Unknown businessCode is not found

- **WHEN** 合法 JWT 请求目录中不存在的 `businessCode`
- **THEN** 系统 MUST 返回 404（或项目等价业务码），MUST NOT 调用 TextToVoice

#### Scenario: Dimension custom is rejected

- **WHEN** 请求 `productScope=dimension` 且 `voiceKind=custom`
- **THEN** 系统 MUST 返回 400，MUST NOT 查库合成

#### Scenario: Custom voice requires owner

- **WHEN** 请求 `productScope=companion` 且 `voiceKind=custom`，且该 `businessCode` 不属于当前 JWT `sub`
- **THEN** 系统 MUST 返回 403 或 404，MUST NOT 返回他人样音

#### Scenario: Concurrent first preview dedupes

- **WHEN** 两请求同时对同一音色首次试听导致唯一键冲突
- **THEN** 冲突方 MUST 再查库并返回已有 PCM，MUST NOT 以 5xx 失败

### Requirement: Preview sample text and PrimaryLanguage follow catalog language

系统 MUST 按预设目录 `language`（`companion_voice.language` / `dimension_voice.language`）选择试听文案与腾讯 TextToVoice `PrimaryLanguage`。规范化规则 MUST 为：trim 后大小写不敏感，以 `en` 为前缀则视为英文，否则视为中文（含空值与 `zh-cn`）。英文 MUST 使用配置的英文固定文案（默认 `Sunlight through the trees. Let this voice open a gentle moment.`）且 `PrimaryLanguage=2`。中文 MUST 使用配置的中文固定文案（默认 `树影婆娑，阳光正好。让声音为你打开一段温柔时光。`）且 `PrimaryLanguage=1`。定制试听（`voiceKind=custom`）MUST 使用中文文案与 `PrimaryLanguage=1`，MUST NOT 按 `source_language` 分支。系统 MUST NOT 接受客户端覆盖文案或 `language`。落库 `sample_text` MUST 等于本次合成所用文案。

#### Scenario: zh-cn preset synthesizes Chinese

- **WHEN** 预设目录 `language` 为 `zh-cn`（或空、或缺省中文）且缓存未命中
- **THEN** TextToVoice `Text` MUST 等于中文配置文案，且 `PrimaryLanguage` MUST 为 1

#### Scenario: en-us preset synthesizes English

- **WHEN** 预设目录 `language` 为 `en-us`（或其它 `en*`）且缓存未命中
- **THEN** TextToVoice `Text` MUST 等于英文配置文案，且 `PrimaryLanguage` MUST 为 2

#### Scenario: Custom preview stays Chinese

- **WHEN** 请求 `voiceKind=custom` 且缓存未命中
- **THEN** TextToVoice `Text` MUST 等于中文配置文案，且 `PrimaryLanguage` MUST 为 1
