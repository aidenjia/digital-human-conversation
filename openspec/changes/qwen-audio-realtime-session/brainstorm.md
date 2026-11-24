## Design Summary

用 `qwen-audio-3.0-realtime-flash` 走百炼 **WebSocket Realtime** 做端到端语音对话；客户端用声网 RTC **只开 3A + 播放参考通路**，麦不推给频道他人。本服务只提供两个短 HTTP：① RTC 进房凭据（无 ConvoAI agent）；② 百炼临时 API Key（客户端直连百炼）。**不做** WebSocket 中继。现有 `POST|DELETE /api/convoai/agents` 保持不变。

## Alternatives Considered

### Alternative A: 本服务 WebSocket 中继
- **Approach**: 客户端连本服务 WS；本服务持永久 Key 连百炼并转发事件；另发 RTC 凭据给 3A
- **Pros**: 永久 Key 永不进客户端；可统一下发 `session.update`
- **Cons**: 2 副本无粘性时同一次通话绑死单 Pod；滚动发布必断；假死要探两条连接；与 CD「不要 ClientIP 粘性」冲突
- **Why not chosen**: 用户选定临时 Key 直连，避免长连接挂在 conversation 副本上

### Alternative B: 临时 Key + 客户端直连百炼（选定）
- **Approach**: 服务端签发 RTC 凭据 + 调用百炼 `POST /api/v1/tokens` 返回短时 `st-*`；客户端直连 `wss://{WorkspaceId}.cn-beijing.maas.aliyuncs.com/api-ws/v1/realtime?model=qwen-audio-3.0-realtime-flash`
- **Pros**: 本服务仍是短请求；无粘性/中继；延迟更短；永久 Key 留在服务端
- **Cons**: 临时 Key 继承永久 Key 全部权限（仅缩短暴露窗口）；最长 TTL 1800s；客户端自建 WS 探活与重连
- **Why chosen**: 与当前部署拓扑（replicas=2、无 sessionAffinity）和关停约定一致

### Alternative C: 复用现有 ConvoAI join，仅换 LLM/TTS
- **Approach**: 继续 `POST /api/convoai/agents`，指望声网侧接千问
- **Pros**: 客户端改动最小
- **Cons**: join 仍绑定声网 ASR/VAD/腾讯 TTS；无法「只用声网 3A」；与端到端 realtime 模型模型不符
- **Why not chosen**: 已明确要拆开 3A 与对话模型链路

## Agreed Approach

采用 **Alternative B**：两个新 HTTP 端点 + 客户端直连百炼 WebSocket；声网仅作 3A/播放；ConvoAI 双轨保留。

## Key Decisions

1. **模型**：调用名 `qwen-audio-3.0-realtime-flash`（非家族名 `qwen-audio-realtime`）
2. **传输**：百炼 WebSocket（非 AOQ）；因要自备 3A，不用百炼内置 3A
3. **3A**：声网 RTC SDK 默认 AEC/ANS/AGC；禁止关 `che.audio.aec|ans|agc.enable`；下行音频必须走声网播放通路
4. **鉴权模型**：两个新端点均要求现有 Users Bearer；临时 Key 接口服务端用 `DASHSCOPE_API_KEY` 调百炼 tokens API
5. **响应信封**：HTTP 成功/失败统一 `ApiResponse`；临时 Key 明文仅出现在成功 `data`，禁止写日志
6. **RTC 凭据**：复用 `AgoraRtcTokenService.buildUserToken` + `RoomIdGenerator`；响应无 `agentId`；无 leave/ownership
7. **临时 Key TTL**：默认 `1800` 秒（百炼上限）；可配置，合法范围 1–1800
8. **会话配置归属**：服务端在临时 Key 响应中下发建议的 `wsUrl`、`model`、`instructions`、`voice`（从陪伴/次元配置映射人设；音色本期用配置默认千问系统音色，不映射腾讯 `voice_type`）；客户端负责 `session.update` 与音频事件
9. **轮次**：客户端 `server_vad`；欢迎语由客户端在 `session.updated` 后按服务端下发的可选 `greetingText` 自行 `conversation.item.create` + `response.create`（服务端不连百炼）
10. **Non-goals**：不替换 ConvoAI；不做中继；不做腾讯音色→千问复刻迁移；服务端不存 PCM；不引入新中间件

## Open Questions

1. 临时 Key 响应是否在本期就按 `digitalHumanType` 注入陪伴/次元 `instructions`（推荐：是，减少客户端拼人设）。若用户否决，则仅返回 Key + wsUrl，人设全由客户端自备。
2. 新路径命名：推荐 `POST /api/rtc/credentials` 与 `POST /api/bailian/realtime-tokens`（与 `/api/convoai/**` 隔离）。
