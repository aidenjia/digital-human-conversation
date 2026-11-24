## Context

本服务已实现 **StartAIConversation**（`GET /api/v1/trtc/ai-conversations/start`）：Header 鉴权、`TrtcAiConversationService`、`TrtcProperties`、TLS UserSig、`tencentcloud-sdk-java-trtc` 3.1.1289。产品现需**独立**接入腾讯云 **CreateCloudTranscription**（[文档 129058](https://cloud.tencent.com/document/product/647/129058)），在专用房间内启动云端转录（ASR + 自定义消息），与 AI 对话**不编排、不共享房间**。

当前 SDK 3.1.1289 **不含** `CreateCloudTranscription`（仅有 `StartAITranscription` 等旧 API），实现前须升级 SDK 并编译验证。

## Goals / Non-Goals

**Goals:**

- 暴露 **`GET /api/v1/trtc/cloud-transcriptions/start`**：无请求体、客户端不传 `roomId`；鉴权与开聊相同（`userId|fixedToken`）。
- 服务端生成 `strRoomId`（`TrtcRoomIdGenerator`），调用 **CreateCloudTranscription**，返回 **`StartTranscriptionData`**（字段同形于 `StartConversationData`，**独立类型**）。
- 转录机器人使用 **`TRTC_TRANSCRIPTION_USER_ID`**；`SubscribeList` 订阅鉴权解析出的客户端 UserId。
- 仅配置 **AsrParam**（如 `Lang`、`VadSilenceTime`）；**不**设置 `TranslationParam`。
- 复用现有 CAM 密钥（`TRTC_CLOUD_*`）、UserSig 体系（`TRTC_USERSIG_*`）、`TrtcAuthFilter`、`TrtcUpstreamException` / `TrtcConfigurationException` 模式。

**Non-Goals:**

- 与 `StartAIConversation` 同请求或同响应类型编排。
- 停止/查询转录（Stop/Describe Cloud Transcription）。
- 客户端指定 `roomId` 或 JSON body。
- 翻译（`TranslationParam`）。
- 复用 `TRTC_ROBOT_USER_ID` 作为转录进房身份。

## Decisions

1. **HTTP 形态（对齐开聊，独立路径）**  
   - **决策**：`GET /api/v1/trtc/cloud-transcriptions/start`，无 body、无 query。  
   - **理由**：与现网 `ai-conversations/start` 调用习惯一致；转录与开聊路径分离，避免混用。

2. **响应类型 `StartTranscriptionData`**  
   - **决策**：新建 `record StartTranscriptionData(long sdkAppId, String userId, String userSig, String strRoomId, String taskId)`，信封仍为 `ApiResponse<StartTranscriptionData>`。  
   - **备选**：复用 `StartConversationData` —— **拒绝**（产品要求类型语义分离，避免 API 文档与 OpenAPI 生成混淆）。  
   - **字段语义**：`sdkAppId` 为 **UserSig SdkAppId**（`TRTC_USERSIG_SDK_APP_ID`）；`userId`/`userSig` 为**客户端**进房凭据；`taskId` 为 **CreateCloudTranscription** 的 TaskId。

3. **房间与 RoomIdType**  
   - **决策**：每次 start 生成新 32 位十六进制 `strRoomId`；`RoomIdType = 1`（字符串房间），与 AI 开聊一致。  
   - **理由**：客户端不传 roomId；转录房间与对话房间天然隔离（不同 API 调用各生成一间）。

4. **转录机器人身份**  
   - **决策**：`TranscriptionParam.UserId` = `TRTC_TRANSCRIPTION_USER_ID`；UserSig 由 `TLSSigAPIv2` + `TRTC_USERSIG_*` 生成。  
   - **理由**：与 AI 机器人分离，降低同 UserId 多任务冲突风险。

5. **SubscribeList**  
   - **决策**：单元素列表，订阅鉴权 attribute 中的 `clientUserId`。  
   - **理由**：转录目标为当前已鉴权用户音频流。

6. **AsrParam / TranscriptionParam 非敏感项**  
   - **决策**：`AsrParam.Lang`、`VrParam.VadSilenceTime` 来自 `trtc.asr.*` 或环境变量；`MaxIdleTime`、`SendCustomMode` 等来自 `classpath:trtc/transcription-param.json`（可选 loader，模式同 `TrtcJsonConfigLoader`）。  
   - **理由**：与 LLM/TTS 模板策略一致；前端不参与。

7. **TrtcClient 创建**  
   - **决策**：抽取 `TrtcCloudClientFactory`（或 package-private 工厂），`TrtcAiConversationService` 与 `TrtcCloudTranscriptionService` 共用。  
   - **理由**：避免重复 Credential/HttpProfile/region 逻辑。

8. **SDK 版本**  
   - **决策**：升级 `tencentcloud-sdk-java-trtc` 至 Maven Central 上确认包含 `CreateCloudTranscriptionRequest/Response` 的最低版本；`mvn verify` 为门禁。  
   - **理由**：当前 3.1.1289 无法编译新 API。

9. **错误映射**  
   - **决策**：与开聊一致——配置缺失 `TrtcConfigurationException` → 503/422（与全局 handler 一致）；`TencentCloudSDKException` → `TrtcUpstreamException` / 502；鉴权 401。  
   - **理由**：统一运维与客户端处理。

10. **日志**  
    - **决策**：成功/失败日志含 `taskId`、`roomId`、`sdkAppId`、transcriptionUserId、clientUserId；**禁止**完整 UserSig、Asr 敏感载荷。`RequestId` 仅日志，不进 JSON body。

## Risks / Trade-offs

- **[Risk] SDK 升级传递依赖冲突** → **缓解**：固定版本号 + 全量 `mvn verify`。  
- **[Risk] 客户端误用同一房间同时开聊与转录** → **缓解**：文档说明两次 start 产生两个 `strRoomId`；本期不做同房编排。  
- **[Trade-off] 独立房间** → 若产品日后要「同房字幕」，需新变更编排或接受客户端只进转录房。  
- **[Risk] `.env` 模板含示例密钥** → **缓解**：继续禁止日志输出密钥；部署使用真实环境变量覆盖。

## Migration Plan

1. 升级 SDK 并合并代码；在环境注入 `TRTC_TRANSCRIPTION_USER_ID` 及 ASR 相关变量。  
2. 发布应用；用 `GET /api/v1/trtc/cloud-transcriptions/start` 联调，确认返回 `StartTranscriptionData` 且腾讯云返回 TaskId。  
3. 回滚：部署旧制品；无 DB 变更。

## Open Questions

- `TranscriptionParam.SendCustomMode` / `MaxIdleTime` 默认值以腾讯云示例（如 `SendCustomMode=2`、`MaxIdleTime=30`）为准，联调后可写入 `transcription-param.json` 固化。  
- SDK 目标版本号在 tasks 1.1 中通过 `dependency:tree` / 发行说明确定后回写 `design.md` 或 `tasks.md`。
