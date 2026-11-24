## Context

`TrtcCloudTranscriptionService` 当前经 `TrtcCloudTranscriptionApi`（`CommonClient.commonRequest`）调用自研 `cloudapi.CreateCloudTranscriptionRequest`。腾讯云 Java SDK 的 `AbstractModel.toJsonObject` 按 **@SerializedName 与 Java 字段名一致**（PascalCase）反射序列化；自研模型使用 camelCase 私有字段，导致运行时报 `NoSuchFieldException: SdkAppId` 等。

同版本 SDK（3.1.1289）已提供 **`TrtcClient.StartAITranscription`** 及 `StartAITranscriptionRequest` / `TranscriptionParams` / `RecognizeConfig`，与现有开聊 `StartAIConversation` 调用方式一致。

## Goals / Non-Goals

**Goals:**

- 使用 **`TrtcClient.StartAITranscription`** 完成转录任务创建，消除 `cloudapi` 桥接与 `NoSuchFieldException`。
- 保持对外 HTTP 契约不变：`GET /api/v1/trtc/cloud-transcriptions/start`、`StartTranscriptionData`、鉴权、服务端 `strRoomId`、`user_` 前缀转录机器人 UserId。
- 继续仅 ASR（`RecognizeConfig`），不配置翻译。
- 删除 `TrtcCloudTranscriptionApi` 与 `trtc.cloudapi` 包。

**Non-Goals:**

- 不升级 SDK 版本（除非联调发现 API 缺口）。
- 不改变 HTTP 路径或响应类型名。
- 本期不实现 `StopAITranscription` / `DescribeAITranscription`。
- 不恢复 **CreateCloudTranscription**（除非后续产品明确要求且 SDK 提供可用 typed 模型）。

## Decisions

1. **云 API 选型：StartAITranscription**  
   - **决策**：`TrtcCloudClientFactory.createTrtcClient()` → `client.StartAITranscription(request)`。  
   - **理由**：官方模型与序列化路径已验证（开聊同 SDK）；无需 CommonClient。  
   - **备选**：继续 CreateCloudTranscription + 手写 PascalCase 字段 —— **拒绝**（维护成本高且已失败）。

2. **请求字段映射**

   | 行为 | StartAITranscription 字段 |
   |------|---------------------------|
   | SdkAppId | `request.SdkAppId` ← `trtc.sdk-app-id` |
   | 房间 | `RoomId` = `strRoomId`，`RoomIdType` = 1 |
   | 会话（可选） | `SessionId` = `strRoomId`（与开聊一致，便于排查） |
   | 转录机器人 | `TranscriptionParams.UserId` / `UserSig`（`user_` + room 后缀） |
   | 订阅客户端音频 | `TranscriptionParams.TargetUserId` = 鉴权 `clientUserId` |
   | 空闲超时 | `TranscriptionParams.MaxIdleTime` ← `transcription-param.json` |
   | 自定义消息模式 | `TranscriptionParams.TranscriptionMode` ← `SendCustomMode`（模板 JSON） |
   | ASR 语言 / VAD | `RecognizeConfig.Language`、`VadSilenceTime` ← `trtc.asr.*` |

   - **说明**：`SubscribeList`（CreateCloudTranscription）对应 **`TargetUserId`**（StartAITranscription），不再使用 `cloudapi.TranscriptionUserInfoParams`。

3. **依赖注入**  
   - **决策**：`TrtcCloudTranscriptionService` 注入 `TrtcCloudClientFactory`，移除 `TrtcCloudTranscriptionApi`。  
   - **理由**：与 `TrtcAiConversationService` 一致。

4. **日志与错误**  
   - **决策**：日志文案改为 `StartAITranscription`；`TencentCloudSDKException` → `TrtcUpstreamException`；成功日志字段白名单不变（不含完整 UserSig）。  
   - **RequestId** 仅日志，不进 HTTP body。

5. **清理范围**  
   - 删除：`TrtcCloudTranscriptionApi.java`、`trtc/cloudapi/**`。  
   - 更新：`StartTranscription.md`、`pom.xml` 桥接注释（删除或改为 StartAITranscription 说明）。

## Risks / Trade-offs

- **[Risk] StartAITranscription 与 CreateCloudTranscription 产品能力不完全等价** → **缓解**：联调确认字幕/自定义消息行为；文档注明 API 名称变更。  
- **[Risk] `TranscriptionMode` 与旧 `SendCustomMode` 枚举含义需对照文档** → **缓解**：保持 `transcription-param.json` 可配置，联调验证。  
- **[Trade-off] 放弃 CreateCloudTranscription 新特性** → 接受，直至 SDK 提供稳定 typed 客户端且产品需要。

## Migration Plan

1. 实现并本地/测试环境调用 `GET /api/v1/trtc/cloud-transcriptions/start`，确认返回 `taskId` 且无 `NoSuchFieldException`。  
2. 部署后观察日志中 `StartAITranscription` 成功记录。  
3. 回滚：恢复上一版本制品（无 DB 变更）。

## Open Questions

- `SessionId` 是否必须与 `RoomId` 相同：默认与开聊一致设为 `strRoomId`，联调无异常则保持。
