## Why

当前云端转录通过 `CommonClient` + 自研 `cloudapi` 模型调用 **CreateCloudTranscription**，在运行时触发 `AbstractModel.toJsonObject` 的 `NoSuchFieldException`（字段名与 SDK 反射约定不一致）。Maven Central 上的 `tencentcloud-sdk-java-trtc` 3.1.1289 已提供类型安全的 **`TrtcClient.StartAITranscription`**，应改用官方 API 消除桥接层故障并简化维护。

## What Changes

- **实现替换**：`TrtcCloudTranscriptionService` 改为使用 `com.tencentcloudapi.trtc.v20190722.TrtcClient#StartAITranscription`，不再调用 `CreateCloudTranscription` / `CommonClient`。
- **请求映射**：`TranscriptionParams`（转录机器人 `user_` + 房间号 UserId/UserSig、`TargetUserId` 订阅客户端）、`RecognizeConfig`（`Language`、`VadSilenceTime`，无翻译）。
- **移除**：`TrtcCloudTranscriptionApi`、`trtc.cloudapi.*` 手写模型及 `pom.xml` 中 CommonClient 桥接说明。
- **不变**：`GET /api/v1/trtc/cloud-transcriptions/start`、`StartTranscriptionData` 响应形态、Header 鉴权、服务端生成 `strRoomId`、`user_` 前缀转录机器人 UserId、与 AI 开聊隔离。

## Capabilities

### New Capabilities

- `trtc-start-ai-transcription`: 规定转录 start 使用 `StartAITranscription` 的云 API 契约、SDK 模型映射、错误处理与对既有 HTTP/配置行为的保持。

### Modified Capabilities

- `trtc-create-cloud-transcription`: 若主规格目录已归档则通过本变更 supersede；实现层从 CreateCloudTranscription 迁移至 StartAITranscription（对外 HTTP 行为不变）。

## Impact

- **代码**：`TrtcCloudTranscriptionService`（核心）、删除 `TrtcCloudTranscriptionApi` 与 `cloudapi` 包；`TrtcCloudClientFactory` 继续供 `TrtcClient` 创建。
- **配置**：保留 `TRTC_ASR_LANG`、`TRTC_ASR_VAD_SILENCE_MS`、`transcription-param.json`（`MaxIdleTime` 等映射至 `TranscriptionParams`）。
- **依赖**：无需升级 SDK 版本（3.1.1289 已含 `StartAITranscription`）。
- **测试**：更新/补充服务层或 IT 断言；`mvn verify` 全绿。
- **文档**：更新 `StartTranscription.md` 中云 API 名称与 SDK 说明。
