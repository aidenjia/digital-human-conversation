## Why

start 接口仅需鉴权即可拉起对话；`sessionId` 与房间号由服务端统一生成，避免客户端传参与幂等冲突。

## What Changes

- **BREAKING**：`POST /api/v1/trtc/ai-conversations/start` 不再接收请求体（无 JSON 字段）。
- 服务端生成无连字符 UUID 作为 `sessionId`，并作为字符串房间号（`strRoomId`）传给腾讯云。
- 成功响应 `data` 字段不变（`sdkAppId`、`userId`、`userSig`、`strRoomId`、`taskId`）。

## Capabilities

### Modified Capabilities

- `trtc-start-ai-conversation`：start 请求无 body；`sessionId` 服务端生成。

## Impact

- `TrtcAiConversationController`、`TrtcAiConversationService`、集成测试、`StartConversation.md`
