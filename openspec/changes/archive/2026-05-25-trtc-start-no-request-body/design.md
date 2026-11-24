## Context

当前 start 可选传 `sessionId`；房间号已可由服务端生成，但客户端仍可传入 `sessionId`。

## Goals / Non-Goals

**Goals:**

- 控制器无 `@RequestBody`
- 单次调用生成一个 32 位十六进制 ID，同时作为 `SessionId` 与 `RoomId`（`roomIdType=1`）
- 鉴权仍仅依赖 Header

**Non-Goals:**

- 在 HTTP 响应中单独返回 `sessionId` 字段（与 `strRoomId` 同值，客户端用 `strRoomId` 即可）

## Decisions

1. **ID 生成**：`UUID.randomUUID().toString().replace("-", "")`。
2. **腾讯云请求**：`setSessionId(id)` 与 `setRoomId(id)` 使用同一值。

## Risks / Trade-offs

- **[BREAKING]** 依赖请求体 `sessionId` 的客户端需移除 body
