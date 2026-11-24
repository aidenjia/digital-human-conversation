## ADDED Requirements

### Requirement: Preview voice audio by businessCode

系统 SHALL 提供 `GET /api/voice-previews`（JWT Bearer）。Query MUST 含非空 `businessCode`、`productScope`（`companion`|`dimension`）、`voiceKind`（`preset`|`custom`）。系统 MUST 先按 scope/kind 在对应音色目录表用 `business_code` 定位行并得到雪花 `id`（及腾讯 VoiceType）；再以 `(product_scope, voice_kind, voice_ref_id=id)` 查询 `voice_preview_audio`。命中时 MUST 返回库内 PCM，MUST NOT 调用腾讯合成。未命中时 MUST 使用配置的固定试听文案调用腾讯 TextToVoice（`Codec=pcm`，采样率取配置默认 16000），将 PCM 写入 `voice_preview_audio`（含 `sample_text`），再返回。成功时 `ApiResponse.data` MUST 含 `audioBase64`（PCM 的 base64）、`sampleRate`、`codec`（`pcm`）。系统 MUST NOT 将 PCM 写入 COS，MUST NOT 把 PCM 嵌入 voices 列表响应。

#### Scenario: Cache hit returns stored PCM

- **WHEN** 合法 JWT 请求已知预设 `businessCode` 且试听表已有对应行
- **THEN** 系统 MUST 返回 200 与 `audioBase64`/`sampleRate`/`codec`，且 MUST NOT 调用 TextToVoice

#### Scenario: Cache miss synthesizes and stores

- **WHEN** 合法 JWT 请求已知预设 `businessCode` 且试听表无对应行
- **THEN** 系统 MUST 调用 TextToVoice，INSERT 试听行，并返回 PCM 载荷

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

### Requirement: Fixed preview sample text from configuration

系统 MUST 使用服务端配置的固定试听文案作为腾讯 `Text` 与落库 `sample_text`。默认文案 MUST 为：`树影婆娑，阳光正好。让声音为你打开一段温柔时光。` 系统 MUST NOT 接受客户端覆盖该文案（本期）。

#### Scenario: Synthesis uses configured text

- **WHEN** 发生缓存未命中并调用 TextToVoice
- **THEN** 请求 `Text` MUST 等于配置文案，且插入行的 `sample_text` MUST 与之一致

### Requirement: Missing token rejected on voice preview

系统 MUST 对 `GET /api/voice-previews` 要求有效 JWT。

#### Scenario: Voice preview without token is 401

- **WHEN** 未携带有效 JWT 调用 `GET /api/voice-previews`
- **THEN** 系统 MUST 拒绝请求（HTTP 401 且项目信封约定）
