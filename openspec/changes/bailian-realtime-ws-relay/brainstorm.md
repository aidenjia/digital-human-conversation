## Design Summary

将百炼 Qwen-Audio Realtime 的 WebSocket 协议与会话编排**下沉到本服务**，对外提供应用层 WebSocket。前端保留声网 RTC（3A / 播放参考）+ 本服务 WS（上行 PCM、下行音频与字幕事件）。永久 DashScope Key 不出客户端；废除「浏览器直连百炼 + Vite Header 代理」作为正式路径。

必做：**应用层心跳**、**发版/缩容优雅 drain**（对齐 agent 侧 WS 关停经验，conversation 当前仅短 HTTP 需升级）。

## Alternatives Considered

### Alternative A: 维持客户端直连百炼（现状）
- **Approach**: `POST /api/bailian/realtime-tokens` + 客户端直连 `wsUrl`
- **Pros**: 服务无长连接；副本无会话态
- **Cons**: 前端协议/PCM/鉴权代理过重；临时 Key 进浏览器
- **Why not chosen**: 产品明确要减负前端，并把逻辑放后端

### Alternative B: 纯 WS 薄客户端（无声网）
- **Approach**: 音频全程经本服务 WS，不用 Agora
- **Pros**: 前端最轻
- **Cons**: 放弃「声网 3A」产品路径
- **Why not chosen**: 选定方案 1，保留声网 3A

### Alternative C: 本服务 WS 中继 + 声网 RTC（选定）
- **Approach**: 前端 RTC credentials + 本服务 `/ws/...` 中继百炼；心跳 + drain
- **Pros**: 前端变薄；Key 留服务端；可统一 session 参数（smart_turn、历史轮次等）
- **Cons**: 带宽/CPU 上升；需关停与心跳设计；断线重连默认新会话
- **Why chosen**: 满足减负与 3A 双目标

## Agreed Approach

**Alternative C（方案 1）**：协议中继 + 保留 `POST /api/rtc/credentials`；新增鉴权 WebSocket；心跳与优雅下线为同级需求。

## Key Decisions

1. 前端仍调 RTC 凭据进房、mute 推流、走声网播放参考；PCM 经本服务 WS 与百炼交互。
2. 本服务持永久 Key 连百炼；对外 WS 用 Users JWT 鉴权（握手或首帧，禁止把 JWT 长期放 query 明文日志）。
3. 简化客户端事件：上行 `audio.append`（PCM base64）+ `ping`；下行 `audio.delta` / `transcript.*` / `pong` / `error` / `server.draining`。
4. `session.update`（instructions/voice/smart_turn/max_history_turns 等）由服务端配置，首连时下发，客户端不拼百炼原生事件。
5. 单次通话状态绑定建连 Pod；**不保证跨 Pod 续聊**；重连 = 新会话。
6. 心跳：前端↔本服务、本服务↔百炼均需探活与超时关闭。
7. 优雅下线：readiness 摘流 → 通知在途会话 `server.draining` → 关前端 WS → 关百炼上游 → 再退出进程；CD `terminationGracePeriodSeconds` / preStop / lifecycle 需上调并文档化。
8. 现有 `POST /api/bailian/realtime-tokens`：标记为测试/过渡；正式路径改走本服务 WS（可后续 deprecate）。

## Open Questions

1. WS 路径命名：`/ws/bailian/realtime` vs `/api/ws/bailian/realtime`（建议前者，避免与 HTTP `/api/**` 过滤器混淆，但须纳入 Security）。
2. 是否一期就删 Vite `__bailian_ws` 代理（建议实现后 conversation-ui 切新路径再删）。
3. drain 宽限目标秒数（建议对齐 agent：总宽限 ≥30s，应用 drain 可配置，默认 15–25s）。
