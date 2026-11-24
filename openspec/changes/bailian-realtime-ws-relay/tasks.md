## 1. Scaffold & configuration

- [x] 1.1 Extend `BailianRealtimeProperties`（heartbeat interval/idle、drain timeout、WS path、session defaults: smart_turn、max_history_turns、enable_search）
- [x] 1.2 `application.yml` 环境变量占位；local 文档说明
- [x] 1.3 Security：放行/鉴权 `/ws/bailian/realtime`（JWT）；确认与 demo Bearer 范围隔离

## 2. Upstream Bailian WS client

- [x] 2.1 `infrastructure.bailian`：上游 WebSocket 客户端（永久 Key Header、建连、发送 append、收事件、关闭）
- [x] 2.2 单元测试：消息解析/映射（无真实外网可 mock）

## 3. Relay session orchestration

- [x] 3.1 `BailianRealtimeRelaySession`：一客户端 WS ↔ 一百炼 WS；首连 `session.update`；事件映射
- [x] 3.2 会话注册表（进程内）供 drain 遍历
- [x] 3.3 简化客户端协议 DTO（ping/pong、audio.append、audio.delta、transcript、error、server.draining）

## 4. WebSocket endpoint

- [x] 4.1 Handler/Config：握手或首帧鉴权 → 创建 session
- [x] 4.2 缺/无效 token 关连测试
- [x] 4.3 合法建连 + mock 上游 happy path 测试

## 5. Heartbeat & timeouts

- [x] 5.1 客户端腿心跳与 idle 超时关连
- [x] 5.2 上游失败结束客户端会话
- [x] 5.3 超时/上游断开测试

## 6. Graceful drain

- [x] 6.1 Shutdown hook / readiness drain：拒绝新连接、广播 `server.draining`、关闭上下游
- [x] 6.2 更新 `docs/k8s-graceful-shutdown-requirements.md`（conversation 含 WS drain 建议值）
- [x] 6.3 Drain 行为测试（可用假时钟/同步触发）

## 7. Docs & UI switch

- [x] 7.1 README：正式路径 = RTC credentials + 本服务 WS；临时 Key 标过渡
- [x] 7.2 conversation-ui：改连本服务 WS；移除百炼直连与 Vite `__bailian_ws`（或标记废弃）
- [x] 7.3 `mvn verify` + UI 手测清单（通话、字幕、打断、杀进程见 draining）
