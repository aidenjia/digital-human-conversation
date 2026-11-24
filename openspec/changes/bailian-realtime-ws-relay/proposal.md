## Why

客户端直连百炼 Realtime 使前端承担完整协议、PCM 桥与 Header 代理，过重且临时 Key 出浏览器。产品要保留声网 3A，同时把会话编排收到本服务。现 conversation 仅短 HTTP、无 WS drain；引入中继必须同步心跳与优雅下线，避免发版掐通话与假死连接。

## What Changes

**本服务 Realtime WebSocket（新增）**
- From: 客户端直连百炼 `wsUrl`（临时 Key）
- To: 鉴权后连接本服务 WS；服务端持永久 Key 中继百炼；简化前后端事件（音频/字幕/心跳/drain）
- Reason: 减负前端、密钥不出端、统一 session 参数
- Impact: 新长连接能力；CD 关停参数上调；与既有「不做中继」决策 supersede

**心跳与优雅下线（新增）**
- From: conversation 无应用层 WS drain（见 `docs/k8s-graceful-shutdown-requirements.md`）
- To: 双端心跳超时关连；readiness 摘流 + `server.draining` + 关上下游；文档同步 termination/preStop/lifecycle
- Reason: 长连接运维安全
- Impact: 运维配置与 agent 类似需对齐

**RTC 凭据（保留）**
- `POST /api/rtc/credentials` 不变，供前端 3A/播放参考

**临时 Key HTTP（过渡）**
- `POST /api/bailian/realtime-tokens` 保留测试/过渡；README 标明正式路径为本服务 WS；后续可 deprecate

## Capabilities

### New Capabilities
- `bailian-realtime-ws-relay`: 本服务 WebSocket 中继百炼 Realtime（鉴权、事件契约、上游生命周期）
- `bailian-realtime-ws-lifecycle`: 心跳、超时、readiness drain、优雅关停与重连语义

### Modified Capabilities
- （无强制改 `openspec/specs/` 已归档项；`qwen-audio-realtime-session` change 中「不做中继」由本 change supersede 正式路径）

## Impact

- 新模块建议：`api` 或 `websocket` 适配层 + `bailian` 编排 + `infrastructure.bailian` 上游 WS 客户端
- 依赖：Spring WebSocket / 或已有栈；OkHttp/Java-WebSocket 连百炼；不强制 Redis（会话态跟连接）
- Security：WS 必须 JWT；禁止日志打印永久 Key / 临时材料 / 完整 PCM
- CD/文档：更新 graceful shutdown；README 客户端清单改为「RTC + 本服务 WS」
- 测试：鉴权失败、心跳超时、drain 通知、上游断开映射；`mvn verify`
- conversation-ui：切到本服务 WS，移除对百炼直连与 Vite `__bailian_ws` 的依赖（可同 change 或紧随）
