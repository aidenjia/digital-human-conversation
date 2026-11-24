## 1. SDP 信令与票据

- [x] 1.1 新增 `POST /api/bailian/realtime/webrtc` 的请求/响应 DTO 与薄 Controller：只校验 `offerSdp` 非空，`model` 走 `BailianRealtimeModels`，不要求 `persona` / `voice` / `enableAction`，返回 `ApiResponse`
- [x] 1.2 infrastructure 用永久 Key 向 `https://{workspaceId}.cn-beijing.maas.aliyuncs.com/api/v1/webrtc/realtime?model=` POST Offer SDP，成功取出 Answer 正文，失败走现有上游/配置错误且不回写 Key
- [x] 1.3 Redis 逻辑键 `bailian-realtime-webrtc:{relayId}`（NameMapper 加 `digital-human:` 前缀），TTL 60 秒，只保存 userId 与 model；日志不写 SDP、Key、JWT

## 2. 控制 WebSocket

- [x] 2.1 注册 `WS /ws/bailian/realtime-events`，握手复用 Users JWT / demo token；要求 `relayId`、`persona`、`voice`，`enableAction` 解析与 PCM 相同；用户一致则消费票据，空白人设/音色拒绝且不消费票据；不读取 query `model`
- [x] 2.2 事件会话复用现有 session.update / 欢迎语 / 动作工具的 JSON 构造：instructions 为 `## Role` + 握手人设 + 口播规则，voice 用握手值，model 用票据值；`enableAction` 仍在欢迎语结束后注册
- [x] 2.3 客户端转发的百炼事件中丢弃 `response.audio.delta` 且不打 payload；下行保留字幕、打断、动作、`session.ready`、`pong`、`server.draining`，不下发 `audio.delta`；纳入现有 drain
- [x] 2.4 确认 `WS /ws/bailian/realtime` 仍按原 query 建上游并转发 PCM，不要求 `relayId`

## 3. 测试与文档

- [x] 3.1 信令测试：缺 token、无效 token、非法 model 拒绝且不调用上游；合法 flash 缺省与 plus 显式各一条，断言回显 model 且日志无 Key；POST 不因缺少 persona/voice 失败
- [x] 3.2 事件测试：票据匹配且人设、音色非空则接受并删除、二次使用拒绝；过期或用户不符拒绝；空白 persona/voice 拒绝且不消费票据；query `model` 不覆盖票据模型；转发 `session.created` 产出的 `session.update` 含握手人设与音色；`response.audio.delta` 不产生 `audio.delta`
- [x] 3.3 更新 README 的 WebRTC 清单（POST 只有 offerSdp 与 model；控制 WS 带 relayId、persona、voice、enableAction；先连控制再设远端 SDP；DataChannel `oai-events`）；写明本路径不调用 `POST /api/rtc/credentials`、不进声网频道；PCM 中继仍可单独用声网 3A，且 WebRTC 不使用写死人设与 `default-voice`
- [ ] 3.4 `mvn spotless:apply` 与 `mvn verify` 已通过。联调时 flash 与 plus 各确认一次 WebRTC 是否接受 `smart_turn`；若拒绝则仅本路径降为 `server_vad` 并记入本 change。尚未联调，两条路径仍用 `smart_turn`
