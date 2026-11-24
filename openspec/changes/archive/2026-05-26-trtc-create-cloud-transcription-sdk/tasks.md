## 1. SDK 依赖

- [x] 1.1 保持 `tencentcloud-sdk-java-trtc` 与 `tencentcloud-sdk-java-common` 使用 **LATEST**（按产品要求暂不固定版本）
- [x] 1.2 运行 `mvn -q compile` 确认 `CreateCloudTranscriptionRequest`、`TranscriptionParam`、`AsrParam`、`TrtcClient.CreateCloudTranscription` 可解析

## 2. 服务实现

- [x] 2.1 重构 `TrtcCloudTranscriptionService`：组装 `CreateCloudTranscriptionRequest`（`SdkAppId`、`RoomId`、`RoomIdType=1`、`TranscriptionParam`、`AsrParam`）
- [x] 2.2 `TranscriptionParam`：`UserId`/`UserSig`（`user_` + 房间后缀）、`SubscribeList` = 客户端 UserId、`MaxIdleTime`/`SendCustomMode` 来自 `transcription-param.json`
- [x] 2.3 `AsrParam`：`Lang`、`VadSilenceTime` 来自 `trtc.asr`；不设置 `TranslationParam`
- [x] 2.4 调用 `client.CreateCloudTranscription`；映射 `TaskId`；日志/异常文案改为 `CreateCloudTranscription`
- [x] 2.5 删除 `StartAITranscription*` 相关 import 与请求组装代码

## 3. 测试与文档

- [x] 3.1 确认 `TrtcCloudTranscriptionIT` 仍通过（Mock 服务层）；必要时补充 `CreateCloudTranscription` 请求组装单测
- [x] 3.2 更新 `README.md` TRTC 转录小节：云 API 改为 `CreateCloudTranscription`，对比表同步
- [x] 3.3 运行 `mvn -q verify` 全量通过
- [ ] 3.4 （运维）目标环境联调 `GET /api/v1/trtc/cloud-transcriptions/start`，确认返回 TaskId 与 ASR/字幕行为
