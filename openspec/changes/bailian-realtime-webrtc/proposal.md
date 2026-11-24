## Why

PCM 中继把 16 kHz/24 kHz 音频打进 2 副本里的单个 Pod，发版和带宽都压在这条长连接上。百炼对 `qwen-audio-3.0-realtime-flash` 与 `qwen-audio-3.0-realtime-plus` 提供 WebRTC，音频可以在浏览器和百炼之间直传，并自带回声消除。永久 Key 仍不能进浏览器，人设和音色要按这一次通话传入，欢迎语和动作时序要留在服务端。

## What Changes

**SDP 代发**
- From: 无 WebRTC 信令接口；浏览器若直连百炼必须持有 Key
- To: `POST /api/bailian/realtime/webrtc` 用永久 Key 换 Answer SDP，并签发一次性 `relayId`
- Reason: Key 留在服务端，同时避开 `/api/**` 与百炼裸 SDP 的信封冲突
- Impact: 非破坏；新增 JSON API

**人设与音色**
- From: WebRTC 方案若沿用写死黛黛人设和 `default-voice`，客户端无法按通话切换
- To: `persona`、`voice`、`enableAction` 放在控制 WebSocket query 上，规则与 PCM 握手相同；`model` 只在 SDP POST 上确定
- Reason: 会话参数与现有实时语音中继一致，同时信令地址在 WS 连上前就要带模型
- Impact: 非破坏；不改变现有 PCM 握手 query

**控制连接**
- From: 只有 `WS /ws/bailian/realtime`，握手后立刻上行百炼 WebSocket 并转发 PCM
- To: 新路径 `WS /ws/bailian/realtime-events?relayId=&persona=&voice=&enableAction=` 只编排 JSON 事件，不传音频、不再开上游 WebSocket
- Reason: 两条连接的建连副作用不同，不能靠 query 在同一路径上分叉
- Impact: 非破坏；PCM 中继保持原契约。WebRTC 客户端不调用声网凭据、不进频道

## Capabilities

### New Capabilities

- `bailian-webrtc-signaling`: 鉴权后的 SDP 交换、模型白名单、只含 userId 与 model 的 Redis 票据
- `bailian-webrtc-events`: 票据核对用户后，用 query 的人设、音色、动作开关编排 session，不转发 PCM

### Modified Capabilities

- （无）`openspec/specs/` 中没有已归档的百炼 Realtime 需求。本变更不改 ConvoAI，也不改未归档的 PCM 中继需求。

## Impact

- 新 HTTP 与新 WebSocket；复用 JWT、demo token、`BailianRealtimeModels`、口播规则、现有 Redis
- 上游新增 `POST https://{workspaceId}.cn-beijing.maas.aliyuncs.com/api/v1/webrtc/realtime?model=`
- CD 无新中间件、无粘滞、无 UDP 端口
- README 补 WebRTC 客户端清单
- 测试覆盖缺 token、无效 token、非法模型、空白人设/音色、票据不符，以及合法 flash/plus 代发。空白人设/音色在控制 WS 上拒绝
