## Context

`qwen-audio-realtime-session` 已交付 RTC 凭据与临时 Key，约定客户端直连百炼、本服务不中继。联调证明浏览器侧协议与鉴权过重。现改为方案 1：本服务 WS 中继 + 保留声网 3A。deployment 2 副本、无 ClientIP 粘滞；单次通话绑定建连 Pod 可接受。conversation 关停文档当前按「仅短 HTTP」配置，引入 WS 后必须升级 drain。

## Goals / Non-Goals

**Goals:**
- 对外 WebSocket：JWT 鉴权、简化事件、中继百炼 PCM/字幕
- 服务端配置 session（如 smart_turn、max_history_turns、instructions/voice）
- 应用层心跳（客户端↔本服务、本服务↔百炼）
- 优雅下线：readiness + draining 通知 + 关闭上下游
- 保留 `POST /api/rtc/credentials`；更新 README / 关停文档

**Non-Goals:**
- 跨 Pod 会话迁移 / 断线后续聊同一百炼 session
- 替换 ConvoAI join/leave
- 本期强制删除临时 Key HTTP（可标记过渡）
- 本服务做 AEC/存储 PCM
- UE/Unity 生产客户端实现（仅契约）

## Decisions

1. **路径**：`WS /ws/bailian/realtime`（具体以 Security 放行名单为准）。
2. **鉴权**：连接建立时校验 Users Bearer（子协议或首帧 `auth` 事件）；失败关闭并给明确码。
3. **上游**：Pod 内用永久 Key 建连百炼；客户端事件映射为百炼 `input_audio_buffer.append` 等；下行 `response.audio.delta` / transcript 映射为简化事件。
4. **心跳**：可配置 ping 间隔与 idle 超时（建议默认 ping 15s、idle 45–60s）；超时主动关连。
5. **Drain**：监听 Spring 关停 / readiness 开关；对活跃会话发 `server.draining`；等待可配置宽限后强制关闭；更新 `docs/k8s-graceful-shutdown-requirements.md` 与 CD 建议值（conversation 向 agent 的 WS 档位靠拢）。
6. **重连**：客户端收到 draining 或异常关闭后重新 RTC（可选）+ 新开本服务 WS；不恢复历史（服务端可用 max_history 仅单连接内）。
7. **分层**：Handler 薄；`BailianRealtimeRelaySession` 编排；上游 Client 在 infrastructure；配置 `BailianRealtimeProperties` 扩展 heartbeat/drain。
8. **Supersede**：正式客户端路径以本中继为准；直连临时 Key 降为过渡。

## Risks / Trade-offs

- [带宽与 CPU] → 限并发连接数（可配置）、监控、背压关连
- [假死连接] → 双端心跳 + 空闲超时
- [发版中断通话] → drain 通知 + 文档声明；宽限不足则 SIGKILL（运维必须对齐）
- [无粘滞重连] → 接受新会话；不做假粘滞
- [与直连路径双轨] → README 明确正式/过渡，避免客户端混用

## Migration Plan

1. 实现 WS 中继 + 心跳 + drain；本地与 test 验证
2. 更新 CD/关停文档并上线配置
3. conversation-ui 切到本服务 WS
4. 观察稳定性后 deprecate 临时 Key 对外文档（代码可暂留）

## Open Questions

- drain 默认秒数与 prod `terminationGracePeriodSeconds` 最终数值（实现前与运维确认，建议 ≥30s）
- 首帧鉴权 vs 握手子协议：优先选更易与现 Spring Security 集成的一种并在 spec 写死
