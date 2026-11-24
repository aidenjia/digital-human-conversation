## Decisions

1. **格式**：`UUID.randomUUID().toString().replace("-", "")`，32 位小写十六进制。
2. **实现位置**：保留 `TrtcRoomIdGenerator` 作为唯一入口，`TrtcAiConversationService` 不变。
