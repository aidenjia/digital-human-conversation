## Design Summary

在 `/api/convoai/**` 请求入口增加 **MDC Filter**，把 `deviceId` / `agentId` 等关联字段写入 SLF4J MDC，使 Logstash 结构化日志可通过 `mdc.deviceId` 等字段检索整条 join/leave 链路。Service 层在生成 `channel`、`userId` 后补充 MDC；请求结束 `finally` 清理，避免线程池污染。

## Alternatives Considered

### Alternative A: Servlet Filter + ContentCachingRequestWrapper 解析 JSON body

- **Approach**: `ConvoaiMdcFilter` 包装请求，从 join/leave 请求体读取 `deviceId` 或 `agentId` 写入 MDC；`ConvoaiService` 在生成 `channel`/`userId` 后调用 `ConvoaiLogContext.enrich`。
- **Pros**: 入口即有关联字段；鉴权通过后、Controller 之前的 infra 日志也能带 MDC；与 agent `RequestLogContext` 模式一致。
- **Cons**: 需缓存 body，轻微内存开销；须避免在 Filter 打日志或泄露 body。
- **Why not chosen**: —（**选用**）

### Alternative B: 仅在 ConvoaiService 内 bind/clear MDC

- **Approach**: 不用 Filter，在 `join`/`leave` 方法入口 `bind`、出口 `clear`。
- **Pros**: 实现最简单，无 body 重复读取问题。
- **Cons**: `ApiAuthFilter`、`GlobalExceptionHandler`、Filter 链上的日志无 MDC；与 `application-logging.mdc` 中「Filter 入口 bind」方向不一致。
- **Why not chosen**: 覆盖范围不足，无法串联 HTTP 层与上游客户端日志。

### Alternative C: HandlerInterceptor + @ControllerAdvice 后置绑定

- **Approach**: 在 Controller 执行后从已绑定的 `@RequestBody` 写 MDC。
- **Pros**: 不包装 request body。
- **Cons**: 晚于 Filter/部分异常路径；leave/join 之前的基础设施日志仍无关联字段；不如 Filter 直观。
- **Why not chosen**: 时机过晚，无法满足「全链路」目标。

## Agreed Approach

采用 **Alternative A**：新增 `ConvoaiLogContext`（MDC 唯一写入辅助类）+ `ConvoaiMdcFilter`（仅 `/api/convoai/**`）。Filter 顺序在 `ApiAuthFilter` 之后，仅对已放行请求解析 body。`ConvoaiService` 负责 enrich `channel`、`userId`、`digitalHumanType`，join 成功后写入 `agentId`。同步精简 Service 日志消息体中与 MDC 重复的字段。

## Key Decisions

1. MDC 键名：`deviceId`、`userId`、`channel`、`agentId`、`digitalHumanType`（与 `application-logging.mdc` 一致）。
2. 新 capability：`convoai-request-logging`；扩展 `application-logging` delta（MDC 清理义务）。
3. Filter order：`HIGHEST_PRECEDENCE + 12`（在 `ApiAuthFilter` +10 之后）。
4. OPTIONS 预检：不写 MDC，直接放行。
5. 不在 Filter 记录请求体或 Authorization。

## Open Questions

- 无。join 的 `channel`/`userId` 由 Service 生成，Filter 阶段仅能保证 `deviceId`/`digitalHumanType`（join）或 `agentId`（leave）。
