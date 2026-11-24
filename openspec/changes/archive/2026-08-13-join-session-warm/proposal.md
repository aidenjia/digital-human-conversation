## Why

Agent 首轮 chat 故意不 await MemOS traits hydrate，开口第一轮常缺 `## sessionTraits`。Agent 已提供可选 `POST /v1/sessions/warm`；conversation 在 Agora 进房成功后同步代调，可在用户进 RTC / 开口前尽量完成画像预热，且失败不挡 join。

## What Changes

**Join 成功后同步 session warm**
- From: `POST /api/convoai/agents` 仅调用 Agora join 并返回 RTC 凭据
- To: Agora join 成功后同步调用 agent `/v1/sessions/warm`（默认超时 5s）；warm 失败/超时只记 WARN，仍返回既有凭据；响应契约不变
- Reason: 进房窗口内预热 Redis traits，提升首轮画像可用性
- Impact: non-breaking；join 延迟可能增加至多约超时上限；可配置关闭

## Capabilities

### New Capabilities
- `convoai-session-warm`: join 成功后对 digital-human-agent session warm 的同步、best-effort 调用契约（触发点、字段映射、超时、失败降级、配置开关）

### Modified Capabilities
- `convoai-join`: 补充「Agora join 成功后 MUST 触发 session warm（若启用）」行为要求；对外 HTTP 请求/响应形状不变

## Impact

- 代码：`ConvoaiService.join`、新建 `infrastructure` warm client、`ConvoaiProperties` / yml
- API：对外 join/leave 契约不变
- 依赖：OkHttp 调 agent（复用 llm url/api-key）
- 文档：`README.md`、`docs/convoai-client-integration.md` 简述服务端 best-effort warm
- 跨仓：依赖 agent `session-warm` API 已可用
