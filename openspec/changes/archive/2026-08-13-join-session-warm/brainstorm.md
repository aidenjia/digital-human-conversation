## Design Summary

Agent 仓已落地可选 `POST /v1/sessions/warm`（同步 hydrate MemOS traits → Redis，失败不挡 chat）。本 change 在 conversation 网关 **Agora ConvoAI join 成功之后**、返回 RTC 凭据之前，**同步**调用该接口，争取用户开口前首轮即有 `## sessionTraits`。Warm 失败只记 WARN，不改变 join 对外契约（不加可选字段）。

## Alternatives Considered

### Alternative A: Join 内同步 await warm（选定）
- **Approach**: `AgoraRestClient.join` 成功后，同请求线程调用 agent warm；conversation 侧 HTTP 超时默认 5s；成功/失败/超时后均返回既有 `ConvoaiJoinCredentials`
- **Pros**: 进房窗口内尽量完成 hydrate；字段与 `llm.params` 同源，无需客户端再编排；密钥留在服务端
- **Cons**: Join 延迟 += warm RTT（上限约 timeout）；占用 Tomcat 工作线程至超时
- **Why chosen**: 产品明确要求同步；失败降级 + 5s 超时控制尾延迟

### Alternative B: Join 内异步 fire-and-forget
- **Approach**: Join 立即回凭据，后台调 warm
- **Pros**: Join 延迟不变
- **Cons**: 用户快速开口时可能赶不上首轮画像
- **Why not chosen**: 用户选定同步 A

### Alternative C: 客户端自行调 warm
- **Approach**: App 在 join 成功后再打 agent `/v1/sessions/warm`
- **Pros**: conversation 零改动
- **Cons**: 客户端需持有 agent URL + Bearer（密钥外泄）；多一次客户端编排
- **Why not chosen**: 网关已有 llm url/api-key，适合服务端代调

## Agreed Approach

采用 **Alternative A**：仅在 **Agora 进房（REST join）成功之后**同步调用 warm；默认超时 5s；失败/超时只打日志，join 仍成功；不扩展响应体。

## Key Decisions

1. 触发点：`restClient.join` 成功之后、构造/返回 `ConvoaiJoinCredentials` 之前
2. 超时：`convoai.session-warm.timeout-ms` 默认 `5000`
3. 开关：`convoai.session-warm.enabled`（默认 true）
4. 鉴权：复用 `convoai.llm.api-key` Bearer（与 chat completions 一致）
5. URL：由 `convoai.llm.url` 推导 `.../v1/sessions/warm`，或允许显式 `convoai.session-warm.url` 覆盖
6. Body：`clientUserId` / `conversationId(=channel)` / `deviceId` / `digitalHumanType` / 可选 `timezone`
7. 失败语义：不抛、不失败 join；WARN + MDC 关联字段
8. 对外契约：join 响应形状不变

## Open Questions

无（用户已确认超时 5s、失败仅日志、进房成功后调用）。
