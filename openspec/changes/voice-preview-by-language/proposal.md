## Why

配置页预设音色含中文（`zh-cn`）与英文（`en-us`），试听却始终用一句中文调用腾讯 TTS（`PrimaryLanguage` 默认 1）。英文预设（如 gentle voice）听起来不对。目录表已有 `language` 列，join 已用它推导 ASR；试听应对齐同一数据源。

## What Changes

**预设试听按目录语言合成**
- From: 全局一句中文；TextToVoice 不传 `PrimaryLanguage`
- To: 预设读 `language`（`zh-cn`/`en-us`）；`en*` 用英文文案 + `PrimaryLanguage=2`，其余中文 + `1`；缓存 `sample_text` 与目标文案不一致则重合成
- Reason: 试听效果必须匹配音色语言
- Impact: 非破坏；HTTP 契约不变；英文预设已缓存的中文 PCM 会被替换

**定制试听**
- From/To: 不变（一期复刻固定中文；不读 `source_language` 做语言分支）

## Capabilities

### New Capabilities

- （无；行为落在既有 `voice-preview-audio`）

### Modified Capabilities

- `voice-preview-audio`: 预设按目录 `language` 选择试听文案与腾讯 `PrimaryLanguage`；缓存文案不一致则重合成

## Impact

- API：路径 / query / 响应信封不变
- 配置：`digital-human.voice-preview.sample-text-en`（及环境变量）
- CD：无新中间件；可选注入英文文案
- 代码：`voicepreview` + `infrastructure.tts`；不改 `customvoice` / VRS
- 文档：README 试听段补充语言规则
- Non-goals：定制生产、voices 列表加 language、改 join
