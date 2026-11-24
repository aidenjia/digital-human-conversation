## 1. Config and TTS client

- [x] 1.1 `VoicePreviewProperties` + `application.yml` 增加英文试听文案 `sample-text-en`
- [x] 1.2 `TencentTtsClient.textToPcm` 增加 `primaryLanguage`；`RealTencentTtsClient` 调用 `setPrimaryLanguage`

## 2. Catalog resolve and preview orchestration

- [x] 2.1 `ResolvedVoiceRef` 增加可选 `language`；preset 从目录列带出，custom 为 null
- [x] 2.2 语言映射（`en*`→英文否则中文）+ 选文案；放在 `voicepreview.support`，不进 `trtc`
- [x] 2.3 `VoicePreviewService`：按语言合成；缓存 `sample_text` 不一致则 UPDATE 重合成

## 3. Tests and docs

- [x] 3.1 单测：`zh-cn` 中文+PrimaryLanguage=1；`en-us` 英文+2；custom 中文+1；文案不一致重合成；文案一致走缓存
- [x] 3.2 更新 `README.md` 试听段：按目录 language 选文案与 PrimaryLanguage
- [x] 3.3 `mvn spotless:apply` 与 `mvn verify`
