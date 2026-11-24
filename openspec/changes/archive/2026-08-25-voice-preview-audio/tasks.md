## 1. Persistence & config

- [x] 1.1 新增 `VoicePreviewAudio` Entity + Mapper（对齐已有表；投影查询勿默认拉全表大字段策略写清）
- [x] 1.2 新增 `VoicePreviewProperties`（`sample-text` 默认定稿文案、`sample-rate` 默认 16000）并挂 `application.yml` / profiles
- [x] 1.3 可选：仓库备份 DDL 至 `docs/sql/voice-preview-audio.sql`

## 2. TTS infrastructure

- [x] 2.1 新增 `TencentTtsClient`（TextToVoice；复用 `tencent.credentials`；返回 PCM bytes）
- [x] 2.2 单元/集成可 mock：成功解码 Audio、上游失败映射

## 3. Domain orchestration

- [x] 3.1 `VoiceCatalogResolver`：按 scope/kind + businessCode 解析雪花 id、VoiceType、owner；dimension+custom 拒；定制归属校验
- [x] 3.2 `VoicePreviewService`：解析 → 查缓存 → 未命中合成+INSERT（唯一冲突再查）→ 返回结果
- [x] 3.3 Service 单测：命中/未命中/未知 code/非本人定制/并发冲突

## 4. API & security

- [x] 4.1 `VoicePreviewController` + DTO：`GET /api/voice-previews`；`ApiResponse` 含 `audioBase64`/`sampleRate`/`codec`
- [x] 4.2 `ResourceServerSecurityConfig` 保护该路径；鉴权集成测：缺 token / 合法通过
- [x] 4.3 更新 `openid-jwt-validation` 活规格路径列表（实现时与 delta 一致）

## 5. Docs & verify

- [x] 5.1 更新 `README.md`（及规则表若需要）试听契约
- [x] 5.2 `mvn spotless:apply` + `mvn verify`
