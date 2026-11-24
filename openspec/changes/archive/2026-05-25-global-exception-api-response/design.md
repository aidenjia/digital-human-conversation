## Context

- 成功路径：`ApiResponse<T>`，`code=0`，`message="success"`，`data` 为业务载荷（TRTC start 等）。
- 错误路径：`GlobalExceptionHandler` 返回 `ProblemDetail`；`TrtcAuthFilter` 手写 `application/problem+json`；`spring.mvc.problemdetails.enabled=true`。
- 历史决策（`api-prefix-exception-narrowing`）：**禁止** `@ExceptionHandler(Exception.class)` / `Throwable` 兜底，继承 `ResponseEntityExceptionHandler` 处理 Spring MVC 层异常（绑定、消息不可读等），避免误把 404/405 等打成自定义 500。

## Goals / Non-Goals

**Goals:**

- `/api/v1/**` 下控制器与 Filter 产生的错误，JSON 体统一为 `ApiResponse`（与成功响应同一顶层字段）。
- 保留语义化 **HTTP 状态码**（400/401/502/503 等），`code` 字段表达业务/分类错误码。
- 校验失败继续在 `data`（或约定结构）中提供 `errors` 字段级明细。
- 明确回答：**不能**仅靠一个 `@ExceptionHandler(Exception.class)` 覆盖全部场景。

**Non-Goals:**

- 不改变成功响应字段含义。
- 不改造 Actuator、`GET /` 等非业务 API 的错误格式（可继续 Boot 默认 / ProblemDetail）。
- 不在此变更引入 i18n 或多语言 `message` 模板体系。

## Decisions

### 1. 错误信封：`ApiResponse` 扩展

- 在 `ApiResponse` 增加 `fail(int code, String message)` 与 `fail(int code, String message, T data)`。
- **约定**：`code == 0` 仅表示成功；失败时 `code != 0`，`data` 可为 `null` 或结构化明细。
- **HTTP 状态**：由 `ResponseEntity.status(...)` 决定，与 `code` 可一致或分层（例如 HTTP 400 + `code=40001`）。本变更采用 **HTTP 状态与 `code` 数值相同** 的简化方案（400→400，502→502），降低客户端心智负担；若后续需要细分业务码再演进。

### 2. 是否只处理 `Exception` 一种？——**否**

| 来源 | 为何不能只用 `Exception` 兜底 |
|------|-------------------------------|
| `ResponseEntityExceptionHandler` 体系 | `MethodArgumentNotValidException`、`HttpMessageNotReadableException` 等由父类 `protected` 方法处理；若仅注册 `Exception`，需自己复刻大量 Spring 逻辑或破坏默认 404/405 行为。 |
| `TrtcAuthFilter` | 在 Servlet Filter 层，**不经过** `@RestControllerAdvice`；401 须在 Filter 内显式写 `ApiResponse` JSON（或抽取共享 `ErrorResponseWriter`）。 |
| 不同 HTTP 语义 | 配置缺失 503、上游失败 502、校验 400、未授权 401——单处理器仍需内部分支，等价于多处理器 + 更清晰。 |

**推荐结构（折中，非单一 `Exception`）：**

1. **继续** `extends ResponseEntityExceptionHandler`，**override** `handleMethodArgumentNotValid`（及必要时 `handleHttpMessageNotReadable` 等）→ 返回 `ApiResponse<Map<String,String>>` 或专用 `ValidationErrorData`。
2. **保留** `@ExceptionHandler(ConstraintViolationException.class)`（参数校验，父类不覆盖）。
3. **合并领域异常**：引入 `ApiException`（或 `ConversationApiException`）基类，携带 `int code` 与 `String message`；`TrtcConfigurationException`、`TrtcUpstreamException` **改为继承**该基类（或在 handler 中映射）。使用 **一个** `@ExceptionHandler(ApiException.class)` 替代多个 TRTC 专用方法（可选，减少重复）。
4. **可选** `@ExceptionHandler(Exception.class)` **仅**用于未预期运行时异常 → HTTP 500 + `ApiResponse.fail(500, "Internal server error")`，`data=null`，日志记录 stack trace。**与历史「禁止 Exception 兜底」的冲突**：若启用，须在 spec 中 **MODIFIED** `api-routing-policy` 的对应 requirement，并限定为「仅 500 + 不暴露内部细节」，且**不得**拦截已由 `ResponseEntityExceptionHandler` 处理的异常类型（`@Order`、`@RestControllerAdvice` 优先级需谨慎）。**默认建议**：本变更 **不新增** `Exception` 兜底，未捕获异常仍走 Boot 默认 `/error`（可后续单独变更）。

### 3. 弃用 ProblemDetail 作为业务 API 契约

- `GlobalExceptionHandler` 方法返回类型改为 `ResponseEntity<ApiResponse<?>>`（或具体泛型）。
- `Content-Type`: `application/json`（不再要求客户端解析 `application/problem+json`）。
- `spring.mvc.problemdetails.enabled` 可保留，仅影响未自定义处理的非 API 路径。

### 4. `TrtcAuthFilter` 对齐

- 抽取 `ApiResponseWriter`（或静态工具）序列化 `ApiResponse.fail(401, detail)`，替换手写 JSON 字符串，避免转义错误。

### 5. 校验错误 `data` 形态

```json
{
  "code": 400,
  "message": "Validation failed",
  "data": {
    "errors": {
      "name": "must not be blank"
    }
  }
}
```

`message` 为摘要；字段明细在 `data.errors`。

## Risks / Trade-offs

- **[Risk] BREAKING**：依赖 ProblemDetail 的客户端/测试需改写。→ 更新 IT 断言为 `$.code`、`$.message`、`$.data.errors`。
- **[Risk] 与 RFC 9457 工具链不兼容**。→ 文档说明业务 API 以 `ApiResponse` 为准。
- **[Risk] 单一 `Exception` 若实现不当会吞掉框架异常**。→ 不采用「仅 Exception」；保持父类 + 细分 handler。
- **[Trade-off] `code` 与 HTTP status 相同** 时扩展性较弱。→ 后续可加业务子码段。

## Migration Plan

1. 扩展 `ApiResponse` + 共享写入工具。
2. 重构 `GlobalExceptionHandler`（及可选 `ApiException` 基类）。
3. 更新 `TrtcAuthFilter`。
4. 调整集成测试与 README。
5. 通知调用方：错误体从 ProblemDetail 迁移为 `ApiResponse`。

## Open Questions

- 是否在首期增加 **500** 的 `@ExceptionHandler(Exception.class)`（需同步修改 `api-routing-policy` spec）？建议实现阶段由你确认；本提案默认 **不增加**。
- `GET /` 与 Echo 成功响应是否未来也包 `ApiResponse`？**不在本变更**。
