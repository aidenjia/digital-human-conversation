## Context

- 部署 `replicas: 2`，无粘滞；试听 PCM 缓存在 Postgres `voice_preview_audio`（键：`product_scope + voice_kind + voice_ref_id`）
- 中间件已有 Postgres / Redis / COS / 腾讯 TTS；不新增依赖
- 活规格 `voice-preview-audio` 写死全局中文句；`RealTencentTtsClient` 未设 `PrimaryLanguage`
- `companion_voice.language` / `dimension_voice.language` 库值为 `zh-cn`、`en-us`
- 定制一句话复刻 `VoiceLanguage` 文档仅 `1=中文`；生产写死 `zh-CN`。本期不改定制

## Goals / Non-Goals

**Goals:**

- 预设试听按目录 `language` 生成中文或英文 PCM
- 前端仍只传 `businessCode` + `productScope` + `voiceKind`
- 旧缓存若 `sample_text` 与当前语言文案不一致则重合成并更新行
- 错误信封、JWT、PCM 不进 COS / 不进 voices 列表：保持不变

**Non-Goals:**

- 定制生产 / VRS / `sourceLanguage`
- 定制试听按语言分支
- 列表 DTO 增加 `language`
- 客户端覆盖试听文案
- 改 join / ConvoAI TTS 参数

## Decisions

1. **解析**：`ResolvedVoiceRef` 增加可选 `language`；仅 companion/dimension **preset** 从目录列带出；custom 为 `null`（按中文）
2. **映射**：trim + lower-case；`en` 前缀 → 英文（`PrimaryLanguage=2`），否则中文（`1`）。与 `ConvoaiAsrLanguageMapper` 一致，不复制进 `trtc`
3. **文案**：`VoicePreviewProperties` 增加 `sampleTextEn`；中文沿用现 `sample-text`；英文默认 `Sunlight through the trees. Let this voice open a gentle moment.`
4. **TTS**：`TencentTtsClient.textToPcm` 增加 `primaryLanguage`；`RealTencentTtsClient` 调用 `setPrimaryLanguage`
5. **缓存**：命中且 `sample_text` 等于目标文案 → 直接返回；否则合成后 INSERT 或按主键 UPDATE（不改唯一键、不改 DDL）
6. **并发**：INSERT 唯一冲突仍再查；若再查出的文案仍不匹配，以库内行为准返回（与现冲突策略一致，避免循环合成）
7. **包归属**：逻辑留在 `voicepreview` + `infrastructure.tts`；Controller 保持薄

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| 英文 VoiceType 拒 `PrimaryLanguage=2` | 单测覆盖参数；联调用真实 `en-us` 音色验收 |
| 旧英文 PCM 仍是中文 | 文案不一致则 UPDATE |
| 误改定制链路 | spec / tasks 明确不碰 `customvoice` |
| 缓存冲突后再读到旧中文行 | 文档接受；下次请求会因不一致再合成 |

## Migration Plan

1. 发版带英文文案配置与 `PrimaryLanguage`
2. 无需手工清表：英文预设第一次试听会因文案不匹配自动重合成
3. 回滚：去掉语言分支即恢复全局中文（已 UPDATE 的英文行会再因文案不匹配合成回中文）

## Open Questions

无。
