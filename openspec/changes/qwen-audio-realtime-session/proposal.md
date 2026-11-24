## Why

现有 `POST /api/convoai/agents` 会创建声网 ConvoAI 智能体并绑定 LLM/TTS/VAD，无法只拿声网 3A、再用千问端到端 realtime 语音。产品需要 `qwen-audio-3.0-realtime-flash`（WebSocket）+ 客户端自备 3A；本服务只签发 RTC 凭据与百炼临时 Key，避免在 2 副本上挂长连接中继。

## What Changes

**RTC 进房凭据（新增）**
- From: 无「仅 RTC、无 agent」接口
- To: `POST /api/rtc/credentials` 返回 `appId` / `channel` / `token` / `userId`（无 `agentId`）
- Reason: 客户端进房只用声网 3A/播放
- Impact: 非破坏；ConvoAI join/leave 不变

**百炼临时 Key（新增）**
- From: 无百炼 Realtime 相关 API
- To: `POST /api/bailian/realtime-tokens` 返回临时 `token`、`expiresAt`、`wsUrl`、`model` 及建议会话字段（`instructions` / `voice` / 可选 `greetingText`）
- Reason: 客户端直连百炼 WebSocket，永久 Key 留在服务端
- Impact: 非破坏；需新环境变量（API Key、WorkspaceId、默认音色、TTL）

**配置与文档**
- 新增 `bailian.realtime.*` Properties；README / 客户端集成说明补充双端点与客户端直连流程

## Capabilities

### New Capabilities
- `rtc-credentials`: 仅签发声网 RTC 用户进房凭据（无 ConvoAI agent）
- `bailian-realtime-token`: 签发百炼 Realtime 临时 API Key 与客户端建连所需元数据

### Modified Capabilities
- （无）现有 `convoai-join` / `convoai-leave` 需求不改

## Impact

- 新包建议：`api.rtc` / `api.bailian`（Controller + dto）；用例编排可放 `rtc` / `bailian` 包；复用 `AgoraRtcTokenService`、`RoomIdGenerator`、陪伴/次元人设读取
- OkHttp 调百炼 tokens；不引入 Redis/MQ；不新增 WebSocket 服务端
- CD：仅环境变量；无需 sessionAffinity
- 测试：缺 token / 无效 token / 合法通过；百炼上游失败映射；临时 Key 不进日志
- 客户端（UE5/Unity）：直连百炼 WS、Ping/Pong、Key 续期重连（本仓文档约定，本仓不实现客户端）
