## Design Summary

在现有 PCM WebSocket 中继之外，增加浏览器 WebRTC 直连百炼的并行路径。本服务只做两件短/轻的事：用永久 Key 代发 SDP，以及用一条新的控制 WebSocket 编排人设、音色、欢迎语和动作。音频走浏览器与百炼之间的 RTP，不经 Pod。

`POST` 只带 `offerSdp` 与 `model`。`enableAction`、`persona`、`voice` 与 PCM 中继一样放在控制 WebSocket 的 query 上。Redis 票据只绑定 userId 与 model。口播规则仍由服务端拼接。模型仍只允许 `qwen-audio-3.0-realtime-flash` 与 `qwen-audio-3.0-realtime-plus`，未传用 flash。

现有 `WS /ws/bailian/realtime` 保持 PCM 中继，不改 query 契约。WebRTC 控制连接使用新路径 `WS /ws/bailian/realtime-events`。

## Alternatives Considered

### Alternative A: 只做 SDP 代理，状态机搬到客户端

- **Approach**: `POST` 返回 Answer SDP 以及一串 `session.update` JSON，客户端自己把事件写进 DataChannel，并自己处理欢迎语和动作。
- **Pros**: 服务端无长连接；2 副本不需要会话态。
- **Cons**: 欢迎语门闩、动作工具注册时序、确认口播都要在浏览器 / UE 重写一遍。
- **Why not chosen**: 现有 `BailianRealtimeRelaySession` 存在的理由就是把这套时序留在服务端。

### Alternative B: SDP 代理 + 独立控制 WebSocket（选定）

- **Approach**: 媒体客户端直连百炼 WebRTC。`POST /api/bailian/realtime/webrtc` 代发 SDP 并签发 `relayId`。`WS /ws/bailian/realtime-events` 只转发 JSON，复用欢迎语与动作状态机，不向上游再开百炼 WebSocket，不传 PCM。
- **Pros**: 永久 Key 不出服务端；Pod 不再扛 16 kHz/24 kHz PCM；人设、音色按本次通话传入；PCM 中继路径不受影响。
- **Cons**: 仍有一条绑住建连 Pod 的控制连接；动作确认多一跳文本转发；`smart_turn` 在 WebRTC 上需实测。
- **Why not chosen**: 选定项。

### Alternative C: JVM 自己作为 WebRTC 端点

- **Approach**: Pod 内嵌 WebRTC，向百炼发 Offer，客户端仍走现有 PCM WebSocket。
- **Pros**: 客户端协议几乎不变。
- **Cons**: 官方把 WebSocket 定为服务端集成路径；集群要处理 UDP/ICE；带宽不下降；本仓库没有这套原生依赖。
- **Why not chosen**: 与百炼协议选型相反，也解决不了 PCM 经 Pod 的问题。

### Alternative D: 控制事件复用 `WS /ws/bailian/realtime`

- **Approach**: 同一路径用有无 `relayId` 区分 PCM 中继和 WebRTC 控制。
- **Pros**: 少一个路由。
- **Cons**: 现有 Handler 在握手成功后立刻向上游百炼开 WebSocket 并收发音频。两种建连副作用不同，靠 query 分叉容易让 PCM 客户端和 WebRTC 客户端互相踩到。
- **Why not chosen**: 路径分开，鉴权与 drain 可共用，会话处理分开。

## Agreed Approach

**Alternative B**。`model` 只在 SDP POST 上确定，因为百炼信令地址在控制连接之前就要带 `?model=`。`persona`、`voice`、`enableAction` 放在控制 WebSocket query 上，语义与 `WS /ws/bailian/realtime` 相同。服务端不查陪伴 `instruction` / 次元 `personaText`，也不使用写死的黛黛人设或 `bailian.realtime.default-voice`。

调用顺序：

1. 客户端创建 `RTCPeerConnection`，加上麦克风轨，创建 DataChannel `oai-events`，生成 Offer。
2. `POST /api/bailian/realtime/webrtc`（Bearer），body 只有 `offerSdp` 与可选 `model`。服务端用永久 Key 把 Offer POST 到 `https://{workspaceId}.cn-beijing.maas.aliyuncs.com/api/v1/webrtc/realtime?model=`，返回 `answerSdp` 与 `relayId`。
3. 客户端连接 `WS /ws/bailian/realtime-events?access_token=&relayId=&persona=&voice=&enableAction=`。票据核对用户后一次性消费；人设、音色、动作开关以这次 query 为准。query 上的 `model` 不覆盖票据里的模型。
4. 客户端 `setRemoteDescription`。DataChannel 上的百炼 JSON 事件（不含音频 delta）转发到控制 WS；服务端算完要下发的 `session.update` / 欢迎语 / 工具结果，客户端写回 DataChannel。远端音频用 `ontrack` 播放。

## Key Decisions

1. 新 HTTP 与新 WS 都走 Users JWT；`demo-token` 开关打开时与现有 Realtime 路径同一旁路。永久 Key 只出现在服务端到百炼的 SDP 请求上。
2. 对外 SDP 接口使用 `ApiResponse` JSON，不对客户端暴露百炼的裸 `application/sdp`。
3. 控制 WS 的 `persona`、`voice` 必填且 trim 后非空，空白则拒绝握手。`persona` 只是人设正文；服务端加 `## Role` 并拼接 classpath 口播规则。`voice` 是千问音色名，不映射 `voiceBusinessCode` 或腾讯 `voice_type`。
4. `model` 只在 SDP POST 上复用 `BailianRealtimeModels`：空白为 flash，其它非白名单值 400，且不向上游发 SDP。控制 WS 不读取 query `model`。
5. 控制 WS 的 `enableAction` 缺省关闭，`true` / `1` 为开启，语义与 PCM 握手相同。为真时仍在欢迎语完成之后才注册动作工具。
6. Redis 逻辑键 `bailian-realtime-webrtc:{relayId}`（`DigitalHumanRedisNameMapper` 加上 `digital-human:` 前缀），TTL 60 秒，值只有 userId 与 model。握手成功即删除，避免复用。userId 必须与控制连接 JWT `sub` 一致。不新引入中间件。人设不进 Redis。
7. 控制 WS 不连接百炼上游 WebSocket，不接受 `audio.append`，不下发 `audio.delta`。客户端不得把 `response.audio.delta` 转发上来；若转发，服务端丢弃且不打日志正文。
8. 现有 `WS /ws/bailian/realtime` 的 PCM 契约、query `persona`/`voice` 保持不动。
9. 部署仍是 2 副本、无粘滞。SDP 是短 HTTP。控制连接绑建连 Pod，重连等于新会话（重新 POST）。发版时这条连接也要收到 `server.draining`，客户端拆掉 PeerConnection。
10. 日志禁止永久 Key、JWT、完整 SDP、人设正文、PCM。
11. WebRTC 路径不依赖声网。不调用 `POST /api/rtc/credentials`，不进频道。回声消除与降噪用百炼 WebRTC 自带能力。PCM 中继仍可单独使用声网 3A，本变更不删除凭据接口。

## Open Questions

- Qwen-Audio 指南没有单独写 WebRTC 的 `turn_detection` 白名单。现网默认 `smart_turn`。实现时用 flash 与 plus 各测一次；若上游拒绝，仅 WebRTC 路径降为 `server_vad`，PCM 路径保持 `smart_turn`。
- DataChannel 名按 Omni WebRTC 示例使用 `oai-events`。若 Qwen-Audio WebRTC 要求别的名字，以实现时上游行为为准并回写本 change。
