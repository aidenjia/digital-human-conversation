## Why

云端转录当前经 `TrtcClient.StartAITranscription` 实现。产品需改回腾讯云 **CreateCloudTranscription**（[文档 129058](https://cloud.tencent.com/document/product/647/129058)）以匹配字幕/自定义消息等产品能力。此前因旧版 SDK 无 typed API 曾用 `CommonClient` + 自研模型导致 `NoSuchFieldException`；现已引入包含 **`TrtcClient.CreateCloudTranscription`** 的 Maven 依赖，应使用官方类型安全客户端，不再依赖 `StartAITranscription` 或 `cloudapi` 桥接。

## What Changes

- `TrtcCloudTranscriptionService#startTranscription` 改为调用 **`TrtcClient.CreateCloudTranscription`**，请求体使用 `CreateCloudTranscriptionRequest`、`TranscriptionParam`、`AsrParam`（替代 `StartAITranscriptionRequest`、`TranscriptionParams`、`RecognizeConfig`）。
- `pom.xml` 固定 `tencentcloud-sdk-java-trtc` / `common` 为包含 CreateCloudTranscription 的**显式版本**（替换 `LATEST`），`mvn verify` 编译门禁。
- 日志与异常文案由 `StartAITranscription` 改为 `CreateCloudTranscription`。
- **不变**：`GET /api/v1/trtc/cloud-transcriptions/start`、Header 鉴权、无请求体、服务端 `strRoomId`、`user_` 前缀转录机器人 UserId、`StartTranscriptionData` 响应字段、与 AI 开聊隔离。
- 更新 **README** TRTC 转录章节中的云 API 名称。
- 集成测试继续 Mock 服务层；可选补充请求组装单测。

## Capabilities

### New Capabilities

（无 — 行为在既有能力规格上修正实现方式。）

### Modified Capabilities

- `trtc-start-ai-transcription`：云 API 由 `StartAITranscription` 改回 **`CreateCloudTranscription`**（仍经 `TrtcClient` 官方模型，禁止 `CommonClient` / `cloudapi`）。
- `trtc-create-cloud-transcription`：与实现对齐 — typed SDK 调用、转录机器人 UserId 为 **`user_` + 房间后缀**（非全局 `TRTC_TRANSCRIPTION_USER_ID`）、`SubscribeList` 订阅客户端 UserId。

## Impact

- **代码**：`TrtcCloudTranscriptionService`、`pom.xml`、README。
- **规格**：`openspec/specs/trtc-start-ai-transcription`、`trtc-create-cloud-transcription`（归档时合并 delta）。
- **依赖**：升级/固定腾讯云 TRTC Java SDK；无 HTTP 契约破坏性变更。
- **运维**：联调需确认 CreateCloudTranscription 在目标地域与 SdkAppId 下返回 TaskId。
