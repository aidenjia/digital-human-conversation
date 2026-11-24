## Context

`digital-human-conversation` 已配置 `logging.structured.format.console: logstash` 与 `include: [mdc, request]`，但 ConvoAI 关联字段仅写在 `ConvoaiService` 日志消息体中（`deviceId=` 等），无法在 Kibana/日志平台用 `mdc.deviceId` 统一检索。`.cursor/rules/application-logging.mdc` 已预留「在 `/api/convoai/**` 入口 Filter 写 MDC」作为后续增强。

现有 Filter 链：`ApiCultureFilter` (+5) → `ApiVersionFilter` (+7) → `ApiAuthFilter` (+10，仅 `CONVOAI_PREFIX`)。

## Goals / Non-Goals

**Goals:**

- 为 `/api/convoai/**` 同步 HTTP 请求建立 SLF4J MDC 上下文。
- join：尽早写入 `deviceId`、`digitalHumanType`；Service 补充 `channel`、`userId`、`agentId`。
- leave：写入 `agentId`。
- 请求结束必须 `MDC.clear()`，防止 Tomcat 线程复用污染。
- 新增集成测试验证 MDC 绑定与清理。

**Non-Goals:**

- 不改 `/api/echo` 等非 ConvoAI 路由。
- 不引入 agent 项目的 `MemOsRequestContext` 或 `conversationId`。
- 不在 Filter 打印请求体、token 或 Bearer。
- 不改造 Logback 配置文件（继续用 Spring Boot 默认 + yml）。

## Decisions

### 1. `ConvoaiLogContext` 放在 `trtc.support` 包

与 agent 的 `logging.RequestLogContext` 对齐职责，但键集合按 ConvoAI 网关裁剪。提供 `bindJoinFields`、`bindLeaveFields`、`enrichAfterJoin`、`clear`、`runWith` 等静态方法；内部 `putIfText` + `MDC.clear()`。

### 2. `ConvoaiMdcFilter` 使用 `ContentCachingRequestWrapper`

join/leave 的关联字段在 JSON body 中，Filter 必须在不破坏 Controller 读 body 的前提下解析：

- 包装 `HttpServletRequest` 为 `ContentCachingRequestWrapper`。
- 调用 `filterChain.doFilter` 后（或之前读取 cached content）用 `ObjectMapper` 仅解析所需字段（`deviceId`、`digitalHumanType`、`agentId`），**不**记录解析结果到日志。
- POST join：bind `deviceId`、`digitalHumanType`（枚举 name）。
- DELETE leave：bind `agentId`。
- 解析失败（空 body、畸形 JSON）：不 bind，不阻断请求；校验仍由 Controller 负责。

### 3. Filter 注册与顺序

`WebSecurityConfig` 新增 `FilterRegistrationBean<ConvoaiMdcFilter>`：

- `addUrlPatterns(ApiPaths.CONVOAI_PREFIX + "*")`
- `setOrder(Ordered.HIGHEST_PRECEDENCE + 12)`

在鉴权之后执行，避免对 401 响应解析 body；OPTIONS 跳过。

### 4. Service 层 enrich 与日志去重

`ConvoaiService.join` 在生成 `channel`、`userId` 后调用 `ConvoaiLogContext.enrichAfterJoin(...)`，上游返回后 `putIfText(agentId)`。INFO 日志改为事件短语，**不再**重复 MDC 已有字段（如 `deviceId=`），保留 `ConvoAI join requested` / `ConvoAI leave requested` 生命周期句。

`AgoraRestClient` 日志依赖同线程 MDC，无需改签名。

### 5. 异常路径

`GlobalExceptionHandler` 在处理 `/api/convoai/**` 的 5xx 时，若当前线程 MDC 非空，日志自动带关联字段（无需改 handler，SLF4J 已输出 mdc.*）。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| `ContentCachingRequestWrapper` 大 body 内存 | ConvoAI body 极小（<1KB），可接受 |
| Filter 与 Service 双重写 MDC | 统一经 `ConvoaiLogContext`，Service 只 enrich |
| 畸形 JSON 无 MDC | 可接受；400 校验日志不强制 MDC |
| prod 开启 DEBUG 时 body 泄露 | 保持 `application-logging.mdc` 禁令，不在 Filter 打 DEBUG body |
