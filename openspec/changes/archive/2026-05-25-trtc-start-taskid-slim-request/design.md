## Context

当前 start 接口要求客户端传 `roomId`/`roomIdType`，响应 `data` 含进房凭据但不含 `taskId`。

## Goals / Non-Goals

**Goals:**

- 请求体仅可选 `sessionId`
- `data` 含 `sdkAppId`、`userId`、`userSig`、`strRoomId`、`taskId`
- 腾讯云 `RoomId`/`RoomIdType` 由服务端推导

**Non-Goals:**

- 返回 `requestId` 给客户端
- 支持客户端指定数字房间（`roomIdType=0`）

## Decisions

1. **房间号**：`strRoomId = sessionId`（非空时），否则 `UUID.randomUUID()` 去掉 `-` 的 32 位十六进制串；固定 `roomIdType = 1`（字符串房间）。
2. **`taskId`**：来自 `StartAIConversationResponse.getTaskId()`，写入 `data.taskId` 与日志。
3. **空请求体**：允许 `{}` 或省略 body（Spring 反序列化为 `sessionId=null`）。

## Risks / Trade-offs

- **[BREAKING]** 依赖客户端传 `roomId` 的集成方需改为使用响应中的 `strRoomId`
- **sessionId 兼作房间号** → 调用方需保证 `sessionId` 在业务内唯一且 ≤64 字节
