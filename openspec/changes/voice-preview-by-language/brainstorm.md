## Design Summary

配置页预设音色「试听」按目录表 `language` 列（库内为 `zh-cn` / `en-us`）选择腾讯 TextToVoice 的合成文案与 `PrimaryLanguage`，生成对应语言 PCM。前端契约不变：仍只传 `businessCode` + `productScope` + `voiceKind`。定制音色生产链路与定制试听语言分支均不改（一期复刻固定中文）。

## Alternatives Considered

### Alternative A: 只补 PrimaryLanguage，文案仍用中文
- **Approach**: 解析 `language` 后仅设置腾讯 `PrimaryLanguage`（`en*`→2，否则 1），`sample-text` 仍是全局中文句
- **Pros**: 改动最小；缓存键不用动
- **Cons**: 英文预设（如 gentle voice）仍念中文；部分英文-only VoiceType 可能失败
- **Why not chosen**: 产品要的是「生成什么样的音频」，不只是 API 语言标记

### Alternative B: 后端按目录语言选文案 + PrimaryLanguage（选定）
- **Approach**: 预设解析时带出 `language`；`en*` 用英文固定文案 + `PrimaryLanguage=2`，其余中文文案 + `1`；缓存仍按音色一行；`sample_text` 与目标文案不一致则重合成
- **Pros**: 语言以库为准；前端零改；与 join ASR 的 `en*` 前缀规则一致；不引入新中间件
- **Cons**: 须改活规格「一句中文」；英文文案走 yml；旧英文 PCM 可能脏
- **Why chosen**: 契约不变且试听效果正确

### Alternative C: 列表返回 language，前端再传给试听 API
- **Approach**: 扩展 voices 列表 DTO，试听 query 增加 language
- **Pros**: 前端可展示中/英标签
- **Cons**: 试听不需要前端传语言；可伪造；本仓库无 Vue 页
- **Why not chosen**: 展示语言可另开；试听以目录列为准

## Agreed Approach

Alternative B。`VoiceCatalogResolver` 对 **preset** 带出 `language`；`VoicePreviewService` 选文案并传 `PrimaryLanguage`；定制解析不读语言、保持现有中文合成。不改 VRS / `sourceLanguage`。

## Key Decisions

1. **范围**：只改 `GET /api/voice-previews` 预设路径（companion + dimension preset 同一套）
2. **语言源**：`companion_voice.language` / `dimension_voice.language`；规范化后 `en*` → 英文，否则中文（含空值）
3. **库内取值**：以 `zh-cn`、`en-us` 为准（大小写不敏感）
4. **腾讯**：`PrimaryLanguage` 1=中文、2=英文；文案中英各一句，服务端配置，客户端不可覆盖
5. **英文默认文案**：`Sunlight through the trees. Let this voice open a gentle moment.`
6. **缓存**：键不变；命中但 `sample_text` ≠ 当前语言文案 → 视为未命中，重合成（避免 en-us 播旧中文 PCM）
7. **Non-goals**：定制生产、定制试听按语言分支、voices 列表加 language 字段、改 join

## Open Questions

无（用户已确认：只改预览、按 `zh-cn`/`en-us` 判断、英文占位文案、文案不一致则重合成）。
