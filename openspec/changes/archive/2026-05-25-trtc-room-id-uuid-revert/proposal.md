## Why

时间戳+随机数方案已验证可行，但业务要求恢复为无连字符 UUID，与早期约定及客户端习惯一致。

## What Changes

- `TrtcRoomIdGenerator` 改回 `UUID.randomUUID()` 去掉 `-`（32 位十六进制）
- 更新单测、文档；移除纯数字/时间戳相关说明

## Capabilities

### Modified Capabilities

- `trtc-start-ai-conversation`：服务端房间/会话 ID 生成策略

## Impact

- `TrtcRoomIdGenerator`、`StartConversation.md`、相关单测
