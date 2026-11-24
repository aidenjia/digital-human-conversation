## Context

本服务已有两条语音路径：声网 ConvoAI join/leave，以及百炼 Qwen-Audio 的 PCM WebSocket 中继（`WS /ws/bailian/realtime`）。中继在握手时要求 query `persona`、`voice`，并用永久 Key 再连百炼 WebSocket，转发 16 kHz/24 kHz PCM。部署按既有设计为 2 副本、ClusterIP、无粘滞。已有 Postgres 与 Redis，本变更不新增中间件。

百炼协议总览确认 `qwen-audio-3.0-realtime-flash` 与 `qwen-audio-3.0-realtime-plus` 支持 WebRTC。浏览器侧音频走 RTP，事件走 DataChannel；浏览器不能直接拿永久 Key 去 POST SDP（CORS，且 Key 不得出服务端）。官方把 WebSocket 定为服务端集成路径，因此 Java 进程不做 WebRTC 端点。

`README` 仍写人设与音色由服务端配置，与当前中继代码不一致。本变更的 WebRTC 路径以代码为准：人设与音色由客户端传入。PCM 中继契约不动。

## Goals / Non-Goals

**Goals:**

- `POST /api/bailian/realtime/webrtc`：校验 JWT 与模型，代发 SDP，返回 Answer SDP 与一次性 `relayId`
- `WS /ws/bailian/realtime-events`：用 `relayId` 核对用户与模型，query 读取 `persona`、`voice`、`enableAction`，编排 `session.update`、欢迎语、动作工具
- 音频不经本服务；控制连接不向上游再开百炼 WebSocket
- 模型白名单与现有中继相同；口播规则仍服务端拼接
- 鉴权失败、非法模型、空白人设/音色、票据不匹配都有测试意图。空白人设/音色发生在控制 WS 握手，不发生在 SDP POST

**Non-Goals:**

- 不替换 ConvoAI join/leave
- 不修改 `WS /ws/bailian/realtime` 的 PCM 契约
- 不把 JVM 做成 WebRTC 媒体端点，不开放 UDP/ICE
- 不接 AOQ
- WebRTC 路径不做手动 PTT
- 不按 `digitalHumanType` 查库注入人设或音色
- 不映射腾讯 `voice_type` 或陪伴 `voiceBusinessCode` → 千问 `voice`
- 不保证跨 Pod 续聊
- WebRTC 客户端不调用 `POST /api/rtc/credentials`，不进声网频道。该接口仍留给 PCM 中继的 3A，本变更不删除它

## Decisions

1. **两条连接，两个路径。** SDP 是短 HTTP。控制事件用 `WS /ws/bailian/realtime-events`。不在现有 PCM 路径上用 `relayId` 分叉，因为该 Handler 握手成功后会立刻连接百炼上游并收发音频。
2. **参数分面。** POST JSON 只有 `offerSdp`（必填）和 `model`（可选，空白为 flash）。成功 `data` 含 `answerSdp`、`relayId`、回显 `model`。`persona`、`voice`、`enableAction` 放在控制 WS query 上，解析规则与 `WS /ws/bailian/realtime` 相同：人设与音色 trim 后非空，否则握手 400；`enableAction` 仅 `true` / `1` 为开启。控制 WS 的 query `model` 不覆盖票据模型。响应信封用 `ApiResponse`。
3. **票据。** Redis 逻辑键 `bailian-realtime-webrtc:{relayId}`，由 `DigitalHumanRedisNameMapper` 加上 `digital-human:` 前缀。TTL 60 秒。值只有 userId 与 model。控制握手校验 JWT `sub` 与票据 userId 一致后删除票据。过期、缺失、用户不符：拒绝握手，不进入事件循环。人设不写入 Redis。
4. **上游 SDP。** `POST https://{workspaceId}.cn-beijing.maas.aliyuncs.com/api/v1/webrtc/realtime?model={model}`，`Content-Type: application/sdp`，`Authorization: Bearer <永久 Key>`，body 为 Offer 原文。成功 body 作为 `answerSdp`。失败映射为现有上游/配置错误，不把永久 Key 或百炼错误正文里的密钥回给客户端。
5. **instructions。** `## Role` + trim 后的 `persona` + `prompts/realtime-session-rules.md`。动作开启时，欢迎语完成后再追加动作说明与 tools。`voice` 原样写入 `session.update`。轮次、历史轮次、搜索、欢迎语开关与文案仍用 `bailian.realtime.*`。WebRTC 不使用 `RealtimePersonaPrompts.PERSONA` 与 `default-voice`。
6. **控制协议。** 客户端把 DataChannel 上的百炼 JSON 事件（去掉 `response.audio.delta`）发给控制 WS。服务端把要写回 DataChannel 的帧作为下行命令返回，并继续下发现有简化事件：`session.ready`、`speech.started`、`speech.stopped`、`transcript.user`、`transcript.assistant`、`action`、`error`、`pong`、`server.draining`。不下发 `audio.delta`。客户端在 `setRemoteDescription` 之前连上控制 WS，避免 `session.created` 先到而无人消费。
7. **分层。** HTTP Controller 只做校验与 `ApiResponse`。SDP 调用放 `infrastructure`。票据与事件编排放 `bailian`，不把 OkHttp 放进 `api`。消息体构造复用现有 session.update / 欢迎语 / 动作工具的 JSON，发送目标改为「交给客户端写 DataChannel」，而不是本服务的上游 WebSocket。
8. **调用顺序。** POST → 控制 WS（消费票据）→ `setRemoteDescription` → DataChannel `oai-events` 与 RTP。控制 WS 失败时客户端拆掉 PeerConnection。
9. **Drain。** 控制会话纳入现有优雅下线：广播 `server.draining` 后关闭。客户端收到后结束 PeerConnection 与控制 WS。重连必须重新 POST。
10. **不依赖声网。** 百炼 WebRTC 自带回声消除和降噪，浏览器直接采集麦克风并播放 `ontrack`。本路径不签发 RTC 凭据、不加入声网频道。声网仍只服务 ConvoAI 与现有 PCM 中继的 3A。

## Risks / Trade-offs

- [WebRTC 拒绝 `smart_turn`] → 实现时 flash/plus 各测一次；若拒绝，仅本路径降为 `server_vad`，PCM 路径不变
- [DataChannel 名与 Qwen-Audio 不一致] → Offer 里仍创建 `oai-events`。百炼把 `session.created` 等事件推到服务端通道 `txt`，`session.update` 必须写回 `txt`。只听 `oai-events` 时控制连接收不到事件，客户端会在 `session.ready` 上超时
- [`session.created` 早于控制 WS] → 客户端必须先连控制 WS 再设远端 SDP；服务端不缓冲百炼事件（它本来就看不到 RTP/DataChannel）
- [票据 60 秒内未连上] → 握手失败，客户端重新 POST
- [控制连接仍绑 Pod] → 接受；不引入粘滞。发版会打断这一通的事件通道，从而要求客户端拆媒体
- [人设放在 WS query，URL 可能很长] → 与 PCM 中继同一约束；日志仍不写人设正文；人设不进 Redis
- [客户端误把音频 delta 转发上来] → 丢弃且不记录 payload

## Migration Plan

1. 实现 SDP 代发、Redis 票据、控制 WS 与测试；PCM 路径回归仍通过
2. 更新 README：WebRTC 清单与「人设/音色由调用方传入」；标明 PCM 中继仍可用
3. 浏览器客户端先走新路径联调；UE 继续 PCM 中继
4. 回滚：去掉新路由与票据写入即可，旧 WS 与 ConvoAI 不依赖本路径

## Open Questions

- `smart_turn` 在这两款模型的 WebRTC 上是否被接受，以联调结果为准（见 Risks）。
