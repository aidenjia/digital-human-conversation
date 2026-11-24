## Why

客户端进房需要 `sdkAppId`、`userId`、`userSig` 与房间号，而当前接口仅返回腾讯云 `taskId`/`requestId`，与前端/SDK 约定不一致。需统一为带 `code`/`message`/`data` 的响应信封，并在 `data` 中返回进房凭据。

## What Changes

- **BREAKING**：`POST /api/v1/trtc/ai-conversations/start` 成功响应由扁平 `{ taskId, requestId }` 改为 `ResponseBody<StartConversationData>`。
- `data` 字段包含：`sdkAppId`（number）、`userId`（string）、`userSig`（string）、`strRoomId`（string，即请求中的 `roomId`）。
- 成功时 `code = 0`，`message` 为成功说明；`userId`/`userSig` 针对鉴权头解析出的客户端用户，`userSig` 由服务端用 UserSig 密钥生成（与机器人 UserSig 分离）。
- 仍调用腾讯云 StartAIConversation；`taskId`/`requestId` 仅写服务端日志，不出现在响应体。
- 4xx/5xx 错误信封由后续变更 `global-exception-api-response` 统一为 `ApiResponse`（本变更仅规定成功路径）。

## Capabilities

### New Capabilities

- （无）

### Modified Capabilities

- `trtc-start-ai-conversation`：成功响应契约与客户端进房凭据字段。

## Impact

- `TrtcAiConversationController`、`TrtcAiConversationService`
- 新增通用 `ApiResponse<T>`（或等价命名）与 start 专用 data DTO
- `TrtcAiConversationIT`、`StartConversation.md`、OpenSpec 主 spec 增量
