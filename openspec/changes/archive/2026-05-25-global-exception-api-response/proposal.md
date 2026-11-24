## Why

成功接口已统一为 `ApiResponse<T>`（`code` / `message` / `data`），但错误路径仍返回 RFC 9457 `ProblemDetail`（含 `TrtcAuthFilter` 手写的 problem+json）。客户端需维护两套 JSON 形态，且与 `trtc-start-response-envelope` 的「统一信封」目标不一致。应在保持合理 HTTP 状态码的前提下，将业务 API 错误也纳入 `ApiResponse` 信封。

## What Changes

- **BREAKING**：`/api/v1/**` 业务接口的错误响应由 `ProblemDetail` 改为 **`ApiResponse`**（`code != 0`，`message` 非空，`data` 可选承载明细如校验 `errors`）。
- 扩展 `ApiResponse`：增加 `fail(...)` 工厂方法；约定非零 `code` 与 HTTP 状态的关系（见 design）。
- 重构 `GlobalExceptionHandler`：各异常类型映射为 `ResponseEntity<ApiResponse<...>>`；校验类错误在 `data` 中保留字段级 `errors`。
- **对齐鉴权**：`TrtcAuthFilter` 401 响应改为同一 `ApiResponse` JSON 形态（**BREAKING**）。
- 评估是否仅保留 **`@ExceptionHandler(Exception.class)`** 单一入口：结论为 **否**（见 design）；采用「父类处理框架异常 + 少量领域/校验处理器（或 `ApiException` 基类）」的折中方案。
- 更新集成测试（`WebApiFoundationIT`、TRTC IT 等）与 README；`spring.mvc.problemdetails.enabled` 可保留供 Actuator/非 API 路径，但业务 API 错误不再依赖 ProblemDetail 作为主契约。

## Capabilities

### New Capabilities

- `api-error-envelope`：业务 API 错误响应的 `ApiResponse` 信封、`code`/`message`/`data` 语义、与 HTTP 状态码的对应关系。

### Modified Capabilities

- `web-api-foundation`：错误模型由「Problem Details 为主」调整为「业务 API 使用 `ApiResponse` 错误信封」。
- `api-routing-policy`：全局异常处理器的返回类型与禁止宽泛兜底的边界（仍禁止无区分的 `Exception` 大包大揽，除非 design 明确仅用于未预期 500）。

## Impact

- `ApiResponse.java`、`GlobalExceptionHandler.java`、`TrtcAuthFilter.java`
- 测试：`WebApiFoundationIT`、`TrtcAiConversationIT`、`TrtcCloudTranscriptionIT`、`TrtcAuthUnconfiguredIT`
- 文档：`README.md`；`trtc-start-response-envelope` 相关说明中「错误继续 ProblemDetail」的表述需同步
- 客户端/调用方：须按 `ApiResponse` 解析 4xx/5xx（**BREAKING**）
