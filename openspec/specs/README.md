# OpenSpec 当前能力

当前系统提供声网 ConvoAI/RTC 与百炼 WebRTC。服务端公共接口以 [根 README](../../README.md) 为入口。

| 能力 | 说明 |
| --- | --- |
| voice-api-surface | 会话接口集合与前端两种模式 |
| convoai-join | 创建 Agent，统一使用配置默认会话画像，保留 timezone/warm/锁 |
| convoai-leave | 校验 Agent 归属并停止 |
| convoai-session-warm | join 后 best-effort 预热 |
| bailian-webrtc-signaling | SDP 与一次性票据 |
| bailian-webrtc-events | WebRTC 控制、字幕、文本、动作、打断 |
| web-api-foundation | Web、探活、JSON、错误处理 |
| api-routing-policy | 显式业务前缀 |
| api-error-envelope | 统一响应信封 |
| openid-jwt-validation | Users JWT 验签 |
| application-logging / convoai-request-logging | 结构化日志与请求关联 |
| docker-container-runtime | 容器与 profile |
| prometheus-metrics | Actuator 指标 |

旧 TRTC 规格是已下线的历史参考。历史变更存于 `openspec/changes/archive/`，不代表当前接口。删除能力与协议简化记录于 `openspec/changes/retain-agora-bailian-webrtc-only/`。

新接口和破坏性契约修改须同步行为规格、README 与测试；禁止 URL 版本前缀。
