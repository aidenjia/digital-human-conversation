## Context

`digital-human-conversation` 为 ConvoAI HTTP 网关（K8s replicas=2，无粘滞；Postgres + Redis + COS + VRS）。音色目录与用户配置已上线；共享库已创建 `voice_preview_audio`（缓存键为雪花 `voice_ref_id`）。尚无 TextToVoice 试听 API。对外音色键统一为 `businessCode`；次元 voices 列表不返回雪花 id。

## Goals / Non-Goals

**Goals:**

- 独立试听 API：JWT + `businessCode` + `productScope` + `voiceKind`
- 目录解析 → 读/写 `voice_preview_audio`；未命中 TextToVoice（pcm）落库
- 全局固定试听文案（yml）
- 鉴权、错误信封、测试与 README 对齐现有约定

**Non-Goals:**

- 改 DDL / 表内存 `business_code`
- voices 列表嵌 PCM；PCM 存 COS
- 次元定制试听；文案变更自动清缓存
- Flyway；`synthesis_params` 参与唯一键

## Decisions

1. **包结构**：`api.voicepreview`（薄 Controller + DTO）+ `voicepreview`（Service/command/support）+ `infrastructure.tts`（Tencent TextToVoice Client）+ persistence Entity/Mapper。不进 `trtc`。
2. **API**：`GET /api/voice-previews?businessCode=&productScope=&voiceKind=`；成功 `ApiResponse.data`：`audioBase64`、`sampleRate`、`codec`（固定 `pcm`）。不用裸 `audio/*`，以保持统一信封。
3. **解析矩阵**：见 brainstorm；未知 code / 未启用预设 → 404；定制非本人 → 403；dimension+custom → 400。
4. **TTS**：复用 `tencent.credentials`；`VoiceType` 来自目录 `tecent_voice_type_id` / 定制 `provider_voice_ref`；`Text` 来自配置；`SessionId` 每次 UUID 不落库。
5. **并发**：INSERT 撞唯一约束 → 再 SELECT 返回。
6. **配置键**：如 `digital-human.voice-preview.sample-text`、`sample-rate`（默认 16000）；密钥仍只走 `tencent.credentials`。
7. **鉴权**：`openid-jwt-validation` 与 `ResourceServerSecurityConfig` 增加 `/api/voice-previews`。

## Risks / Trade-offs

- [Risk] 定制 VoiceType / FastVoiceType 与文档不一致 → Mitigation：联调确认 `provider_voice_ref`；快照写入 `synthesis_params`；失败返回上游错误映射
- [Risk] `bytea` 体积与 `SELECT *` → Mitigation：Mapper 投影；文案已较短
- [Risk] 文案变更后旧 PCM 仍有效 → Mitigation：本期不自动清；运维手工删行或后续加任务
- [Risk] TTS SDK 依赖与密钥缺失 → Mitigation：配置校验；测试 mock Client

## Migration Plan

1. 确认目标库已有表（已完成）
2. 发版应用；无破坏性 API
3. Rollback：下线接口即可；表可保留

## Open Questions

- 无
