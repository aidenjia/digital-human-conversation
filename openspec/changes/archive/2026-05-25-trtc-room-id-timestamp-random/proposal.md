## Why

UUID 生成在高并发下偏重；需用「时间戳 + ThreadLocalRandom」轻量生成 `strRoomId`，并保证 1 秒内可产出 1 万个不重复 ID。

## What Changes

- 新增 `TrtcRoomIdGenerator`：`currentTimeMillis()` + `Long.toUnsignedString(ThreadLocalRandom.current().nextLong())`
- `TrtcAiConversationService` 使用该生成器作为 `sessionId` / `strRoomId`
- 单元测试验证 1 秒内 1 万次生成无重复

## Capabilities

### Modified Capabilities

- `trtc-start-ai-conversation`：服务端房间/会话 ID 生成策略

## Impact

- `TrtcRoomIdGenerator`、`TrtcAiConversationService`、`StartConversation.md`
