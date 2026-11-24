## 1. 服务层迁移

- [x] 1.1 重构 `TrtcCloudTranscriptionService`：注入 `TrtcCloudClientFactory`，组装 `StartAITranscriptionRequest`（`TranscriptionParams` + `RecognizeConfig`，`TargetUserId` = clientUserId，`SessionId`/`RoomId`/`RoomIdType`）
- [x] 1.2 调用 `TrtcClient.StartAITranscription`，映射 `StartAITranscriptionResponse.getTaskId()`；日志与异常文案改为 `StartAITranscription`
- [x] 1.3 将 `transcription-param.json` 的 `SendCustomMode` 映射到 `TranscriptionParams.TranscriptionMode`（保留 `MaxIdleTime`）

## 2. 清理桥接代码

- [x] 2.1 删除 `TrtcCloudTranscriptionApi` 及 `trtc/cloudapi/**` 全部类
- [x] 2.2 移除 `pom.xml` 中 CreateCloudTranscription / CommonClient 桥接注释；移除 `TrtcCloudClientFactory.createCommonClient()`

## 3. 测试与文档

- [x] 3.1 确认 `TrtcCloudTranscriptionIT` 仍通过（Mock 服务层）；必要时补充 `StartAITranscription` 请求组装的单元测试
- [x] 3.2 更新 `StartTranscription.md`：云 API 改为 `StartAITranscription`，删除 CommonClient 说明
- [ ] 3.3 本地或测试环境联调 `GET /api/v1/trtc/cloud-transcriptions/start`，确认无 `NoSuchFieldException` 且返回 `taskId`
- [x] 3.4 `mvn -q verify` 全绿
