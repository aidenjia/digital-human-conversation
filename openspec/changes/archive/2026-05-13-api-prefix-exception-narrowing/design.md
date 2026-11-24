## Context

项目已启用 `spring.mvc.problemdetails.enabled=true`，并存在 `GlobalExceptionHandler` 与 `EchoController`。需在不大改对外 URL 的前提下统一前缀来源，并收窄异常兜底。

## Goals / Non-Goals

**Goals:**

- 业务 REST 控制器集中在 `...api` 包时，对外路径统一为 **`/api/v1/**`**，控制器内只维护资源段（如 `/echo`）。
- 异常处理与 **Spring `ResponseEntityExceptionHandler`** 对齐，避免重复实现与 `Exception` 大包大揽。
- `GET /`、Actuator 路径行为不变。

**Non-Goals:**

- 不引入网关、不修改 `server.servlet.context-path`（避免 Actuator 与根路径被整体搬迁）。
- 不在此变更中拆分多模块或引入 OpenAPI。

## Decisions

1. **前缀实现**  
   - 使用 `WebMvcConfigurer#configurePathMatch` + `addPathPrefix("/api/v1", HandlerTypePredicate.forBasePackage("com.jupiter.digital.human.api"))`。  
   - **理由**：按包裁剪，避免给 `web.RootController` 误加前缀；比全局 `context-path` 更精细。

2. **异常处理**  
   - `GlobalExceptionHandler extends ResponseEntityExceptionHandler`，**override** `handleMethodArgumentNotValid` 以保留 `errors` 扩展字段；**删除** `@ExceptionHandler(Exception.class)`。  
   - **理由**：父类已覆盖 `HttpMessageNotReadableException`、绑定异常等常见 Web 层错误，并与 Problem Details 集成；移除 `Exception` 可避免误把本应 404/405 等交给默认链路的场景打成自定义 500。

3. **仍保留 `@ExceptionHandler(ConstraintViolationException.class)`**  
   - **理由**：参数校验（`@Validated` + 方法参数）产生的约束异常通常不由 `ResponseEntityExceptionHandler` 的同一批模板方法覆盖，保留显式处理更稳。

## Risks / Trade-offs

- **[Risk]** 新增 `@RestController` 若误放在 `api` 包外，将不会自动带 `/api/v1`。  
  - **缓解**：README 明确约定；代码评审关注包名。
- **[风险]** 未捕获的运行时异常将走默认 `/error` 表现，与此前「一律 500 统一文案」可能略有差异。  
  - **缓解**：依赖 Boot 3 + Problem Details 的默认 JSON；生产可再配合观测与告警。

## Migration Plan

- 合并后无需数据迁移；对外 `POST /api/v1/echo` 保持不变。
- 若存在反向代理按路径路由，无需调整（路径未变）。

## Open Questions

- 若未来存在第二版本 API，可再增加 `...api.v2` 包与第二条 `addPathPrefix("/api/v2", ...)`。
