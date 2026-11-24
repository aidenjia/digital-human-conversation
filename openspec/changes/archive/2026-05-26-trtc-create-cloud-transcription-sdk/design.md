## Context

`TrtcCloudTranscriptionService` 当前调用 **`StartAITranscription`**（`TranscriptionParams` + `RecognizeConfig`，`TargetUserId` = 客户端 UserId）。历史变更 `trtc-start-ai-transcription` 曾因 `CommonClient` + 自研 `cloudapi` 反射字段名不一致导致 `NoSuchFieldException`，故迁移至 StartAITranscription。

现 `pom.xml` 已可依赖包含 **CreateCloudTranscription** 的 `tencentcloud-sdk-java-trtc` 版本（仓库中曾注释 `3.1.1468`）。产品要求恢复 **CreateCloudTranscription** 云 API，同时保持对外 HTTP 与进房字段语义不变。

## Goals / Non-Goals

**Goals:**

- `TrtcCloudClientFactory.createTrtcClient()` → **`client.CreateCloudTranscription(request)`**。
- 请求映射：`TranscriptionParam`（机器人 UserId/UserSig、`SubscribeList`、`MaxIdleTime`、`SendCustomMode`）+ **`AsrParam`**（`Lang`、`VadSilenceTime`）；**不**设置 `TranslationParam`。
- 固定 SDK 版本号，`mvn -q verify` 通过。
- 保持 `user_` + 房间后缀转录机器人 UserId、`TrtcRoomIdGenerator`、客户端 UserSig 仅出现在 HTTP 响应。

**Non-Goals:**

- 恢复 `TrtcCloudTranscriptionApi`、`trtc.cloudapi` 或 `CommonClient.commonRequest`。
- 变更 HTTP 路径、鉴权、响应类型或 AI 开聊流程。
- Stop/Describe 转录 API。

## Decisions

1. **云 API：CreateCloudTranscription（typed TrtcClient）**  
   - **决策**：使用 `CreateCloudTranscriptionRequest` / `CreateCloudTranscriptionResponse`，与 `StartAIConversation` 同模式。  
   - **备选**：继续 `StartAITranscription` —— **拒绝**（产品要求 CreateCloudTranscription）。  
   - **备选**：CommonClient + 手写模型 —— **拒绝**（已证明序列化失败）。

2. **字段映射（相对当前 StartAITranscription 实现）**

   | 当前 (StartAITranscription) | 目标 (CreateCloudTranscription) |
   |----------------------------|----------------------------------|
   | `SdkAppId` | `SdkAppId` |
   | `RoomId` + `RoomIdType=1` | `RoomId` + `RoomIdType=1` |
   | `SessionId` | *不设置*（该 API 无此字段） |
   | `TranscriptionParams` | `TranscriptionParam` |
   | `UserId` / `UserSig`（机器人） | `TranscriptionParam.UserId` / `UserSig` |
   | `TargetUserId` | `TranscriptionParam.SubscribeList` = `[clientUserId]` |
   | `MaxIdleTime`, `TranscriptionMode` | `MaxIdleTime`, `SendCustomMode`（来自 `transcription-param.json`） |
   | `RecognizeConfig.Language` | `AsrParam.Lang` |
   | `RecognizeConfig.VadSilenceTime` | `AsrParam.VadSilenceTime` |

3. **转录机器人 UserId**  
   - **决策**：继续 **`TrtcTranscriptionUserIdGenerator.fromRoomId(strRoomId)`**（`user_` 前缀，总长 ≤32）。  
   - **理由**：与现网行为一致，避免全局固定 UserId 多任务冲突；主 spec 中「deployment-scoped `TRTC_TRANSCRIPTION_USER_ID`」在 delta 中修正。

4. **SDK 版本**  
   - **决策**：`tencentcloud-sdk-java-trtc` 与 `tencentcloud-sdk-java-common` **同版本显式 pin**（tasks 中通过 `mvn dependency:tree` 与编译引用 `CreateCloudTranscriptionRequest` 确定，候选 ≥ 3.1.1468）。  
   - **禁止**：`LATEST` 浮动坐标（与 `trtc-start-ai-conversation` 主 spec 一致）。

5. **错误与日志**  
   - **决策**：`TencentCloudSDKException` → `TrtcUpstreamException`；成功/失败日志前缀 `CreateCloudTranscription`；不记录完整 UserSig。

## Risks / Trade-offs

- **[Risk] StartAITranscription 与 CreateCloudTranscription 产品行为差异** → **缓解**：联调验证字幕/自定义消息；文档注明 API 名称。  
- **[Risk] SDK 升级传递依赖冲突** → **缓解**：固定版本 + 全量 `mvn verify`。  
- **[Risk] SubscribeList 模型类型（String[] vs List）** → **缓解**：以 SDK 生成模型为准编译；单测覆盖组装。  
- **[Trade-off] 主 spec 中 `TRTC_TRANSCRIPTION_USER_ID` 文档过时** → 本变更 delta 修正为 room-scoped `user_` 策略。

## Migration Plan

1. 升级并 pin SDK；重构 `TrtcCloudTranscriptionService`；`mvn verify`。  
2. 部署后调用 `GET /api/v1/trtc/cloud-transcriptions/start` 联调，确认 TaskId 与 ASR 回调。  
3. 回滚：部署上一制品（仍使用 StartAITranscription 的版本）。

## Open Questions

- 最终 pin 的 SDK 次版本号在 tasks 1.1 完成后写入 `tasks.md`（以 Maven Central 与本地 `mvn compile` 为准）。
