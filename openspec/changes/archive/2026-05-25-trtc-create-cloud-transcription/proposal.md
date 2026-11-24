## Why

业务需要在服务端独立调用腾讯云 TRTC **CreateCloudTranscription**（开始云端转录），在专用房间内对客户端音频做 ASR，并通过 TRTC 自定义消息下发字幕/转录结果。该能力与现有 **StartAIConversation**（AI 对话）解耦，避免与开聊流程、房间号、任务生命周期混在同一接口中。

## What Changes

- 新增 **`GET /api/v1/trtc/cloud-transcriptions/start`**：无请求体、不传 `roomId`；鉴权方式与开聊一致（Header `userId|fixedToken`）；服务端生成 `strRoomId` 并调用 **CreateCloudTranscription**。
- 新增响应载荷类型 **`StartTranscriptionData`**（字段与 `StartConversationData` 同形：`sdkAppId`、`userId`、`userSig`、`strRoomId`、`taskId`），**不**复用 `StartConversationData` 类型名。
- 转录进房机器人使用独立配置 **`TRTC_TRANSCRIPTION_USER_ID`**（与 `TRTC_ROBOT_USER_ID` 分离）。
- **仅 ASR**：配置 `AsrParam`；**不**下发 `TranslationParam`。
- **本期范围**：仅实现 **CreateCloudTranscription**；不包含停止/查询转录、不与开聊接口编排。
- **依赖**：升级 `tencentcloud-sdk-java-trtc` 至包含 `CreateCloudTranscription` 的版本（当前 3.1.1289 不含该 API）。

## Capabilities

### New Capabilities

- `trtc-create-cloud-transcription`: 覆盖独立 HTTP 入口、Header 鉴权与客户端 UserId 解析、转录机器人 UserId/UserSig、服务端生成房间号、CreateCloudTranscription 请求组装（TranscriptionParam + AsrParam）、`StartTranscriptionData` 响应、配置与日志红线、上游错误映射。

### Modified Capabilities

- （无）既有 `trtc-start-ai-conversation`、`api-routing-policy`、`web-api-foundation` 行为不变；新路径挂在 `con.aidenjia.digitalhuman.conversation.api` 下并沿用 `/api/v1` 与 TRTC 鉴权过滤器前缀。

## Impact

- **代码**：`TrtcCloudTranscriptionController`、`TrtcCloudTranscriptionService`、可选 `TrtcCloudClientFactory`（抽取共用 `TrtcClient` 创建）、`StartTranscriptionData`、`TrtcProperties` 扩展（transcription / asr）。
- **配置**：环境变量 `TRTC_TRANSCRIPTION_USER_ID`、`TRTC_ASR_LANG`、`TRTC_ASR_VAD_SILENCE_MS`；可选 `classpath:trtc/transcription-param.json`。
- **依赖**：腾讯云 TRTC Java SDK 版本升级（阻塞项）。
- **测试**：MockMvc 鉴权与成功路径；`TrtcClient` Mock 隔离真实云调用。
- **文档**：可新增 `StartTranscription.md` 或扩展运维说明；`createTransaction.md` 为需求来源参考。
