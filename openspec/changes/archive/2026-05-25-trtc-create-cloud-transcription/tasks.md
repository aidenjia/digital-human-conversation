## 1. SDK 与配置

- [x] 1.1 升级 `tencentcloud-sdk-java-trtc` 至包含 `CreateCloudTranscription` 的固定版本，`mvn -q compile` 通过
- [x] 1.2 在 `TrtcProperties` 增加 `transcription.user-id`、`asr.lang`、`asr.vad-silence-time`（及可选 `transcription.param-config-path`），绑定 `TRTC_TRANSCRIPTION_USER_ID`、`TRTC_ASR_LANG`、`TRTC_ASR_VAD_SILENCE_MS`
- [x] 1.3 新增 `classpath:trtc/transcription-param.json` 模板（`MaxIdleTime`、`SendCustomMode` 等），更新 `.env.local` / `.env.test` / `.env.dev` 键名说明（含 `TRTC_TRANSCRIPTION_USER_ID`，与 `TRTC_ROBOT_USER_ID` 区分）

## 2. 共用 TRTC 客户端

- [x] 2.1 抽取 `TrtcCloudClientFactory`（Credential + `trtc.tencentcloudapi.com` + region），`TrtcAiConversationService` 改为使用该工厂（行为不变）
- [x] 2.2 配置校验扩展：转录 start 前校验 `TRTC_TRANSCRIPTION_USER_ID` 与 ASR 必填项，缺失时 `TrtcConfigurationException`

## 3. 转录领域服务

- [x] 3.1 实现 `TrtcCloudTranscriptionService#startTranscription(clientUserId)`：生成 `strRoomId`、`RoomIdType=1`、转录 bot UserSig、客户端 UserSig
- [x] 3.2 组装 `CreateCloudTranscriptionRequest`（`TranscriptionParam` + `SubscribeList` + `AsrParam`，无 `TranslationParam`），调用 `CreateCloudTranscription`
- [x] 3.3 映射 `TencentCloudSDKException` 为 `TrtcUpstreamException`；成功日志仅白名单字段；`RequestId` 不进响应

## 4. HTTP 层

- [x] 4.1 新增 `StartTranscriptionData` record（`sdkAppId`, `userId`, `userSig`, `strRoomId`, `taskId`）
- [x] 4.2 新增 `TrtcCloudTranscriptionController`：`GET /api/v1/trtc/cloud-transcriptions/start`，从 request attribute 取 `clientUserId`，返回 `ApiResponse<StartTranscriptionData>`
- [x] 4.3 确认 `TrtcAuthFilter` 已覆盖新路径，无需 body 校验

## 5. 测试与文档

- [x] 5.1 新增 `TrtcCloudTranscriptionIT`（Mock `TrtcCloudTranscriptionService` 或 `TrtcClient`）：鉴权失败 401、成功 200 且 `data` 含五字段
- [x] 5.2 新增 `StartTranscription.md`（或等价运维文档）：接口说明、环境变量、与开聊隔离说明、客户端双房间提示
- [x] 5.3 `mvn -q verify` 全绿
