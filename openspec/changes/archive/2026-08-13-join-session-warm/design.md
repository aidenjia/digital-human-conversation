## Context

- conversation 是 Agora ConvoAI HTTP 网关；join 已向 agent LLM 透传 `llm.params`（含 `clientUserId`、`deviceId`、`digitalHumanType`、`conversationId`、可选 `timezone`）。
- digital-human-agent 提供 `POST /v1/sessions/warm`：请求线程同步 hydrate MemOS traits；响应仅 `ApiResponse`（成功 `code==0`，失败非 0）；非 chat 硬门槛。
- 目标：进房成功后、客户端拿到凭据进 RTC / 开口前，尽量完成 traits 预热。

## Goals / Non-Goals

**Goals:**

- Agora REST join 成功后同步调用 warm
- Best-effort：失败/超时不失败 join；只 WARN
- 配置外置：开关、超时（默认 5s）、可选显式 warm URL
- 分层：HTTP 客户端在 `infrastructure`；编排在 `ConvoaiService`

**Non-Goals:**

- 不扩展 join 响应字段（如 `sessionWarmed`）
- 不把 warm 做成独立对外 API
- 不改 agent 仓契约；不假 stream 预热
- 不在 warm 失败时回滚 Agora agent（leave）

## Decisions

1. **触发点**：仅 `AgoraRestClient.join` 返回成功之后调用；Agora 失败则不 warm。
2. **同步模型**：在 join 请求线程 await warm 完成或超时，再返回凭据。
3. **超时**：OkHttp call timeout 默认 5000ms；超时视为 warm 失败降级。
4. **URL**：优先 `convoai.session-warm.url`；否则把 `convoai.llm.url` 中 `/v1/chat/completions` 替换为 `/v1/sessions/warm`（无法推导且无显式 URL 时跳过并 WARN）。
5. **Client**：新建 `AgentSessionWarmClient`（OkHttp + Bearer），不复用 `AgoraRestClient`。
6. **日志**：INFO `session warm requested` / 成功；WARN 失败（httpStatus / code / 截断 message / durationMs）；禁止记录 api-key。
7. **测试**：Service 单测 mock warm client——成功路径调用一次；warm 抛错/返回失败时 join 仍成功；enabled=false 不调用。

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| Join P99 变差 | 默认 5s 硬上限；可配置；可关开关 |
| Tomcat 线程被占 | 超时上限；监控 join 耗时 |
| llm.url 形态变化导致推导失败 | 支持显式 warm URL；失败跳过并 WARN |

## Migration Plan

- 配置：dev/test 默认可推导；prod 确认 `CONVOAI_LLM_URL` / `CONVOAI_LLM_API_KEY` 可用；可选设 `CONVOAI_SESSION_WARM_*`。
- 回滚：`convoai.session-warm.enabled=false` 即关闭，无需发版回退逻辑以外的配置即可降级。

## Open Questions

无。
