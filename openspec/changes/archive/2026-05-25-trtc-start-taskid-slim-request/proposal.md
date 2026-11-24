## Why

调用方无需再指定房间号：房间由服务端分配并随响应返回；同时需要在 `data` 中带回腾讯云 `taskId`，便于客户端关联对话任务。

## What Changes

- **BREAKING**：请求体移除 `roomId`、`roomIdType`；仅保留可选 `sessionId`（幂等/会话标识）。
- 服务端确定 TRTC 房间：有 `sessionId` 时以其作为字符串房间号（`roomIdType=1`），否则生成 UUID 字符串房间号。
- **BREAKING**：成功响应 `data` 增加 `taskId`（腾讯云 `TaskId`）；`strRoomId` 为服务端分配的房间号（不再来自客户端 `roomId`）。

## Capabilities

### New Capabilities

- （无）

### Modified Capabilities

- `trtc-start-ai-conversation`：start 请求体与成功响应 `data` 字段。

## Impact

- `TrtcAiConversationController`、`TrtcAiConversationService`、`StartConversationData`
- 集成测试与 `StartConversation.md`
