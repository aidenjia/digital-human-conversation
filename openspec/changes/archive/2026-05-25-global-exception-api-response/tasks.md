## 1. ApiResponse 与共享工具

- [x] 1.1 为 `ApiResponse` 增加 `fail(int code, String message)` 与 `fail(int code, String message, T data)`
- [x] 1.2 新增 `ValidationErrorData`（或等价 record，含 `Map<String,String> errors`）供校验错误 `data` 使用
- [x] 1.3 新增 `ApiResponseWriter`（或工具类），供 Filter 与 Advice 统一写 JSON

## 2. GlobalExceptionHandler 重构

- [x] 2.1 将 `handleMethodArgumentNotValid` 改为返回 `ResponseEntity<ApiResponse<ValidationErrorData>>`（`code=400`，`data.errors`）
- [x] 2.2 将 `handleConstraintViolation` 改为 `ApiResponse` 形态
- [x] 2.3 将 `TrtcConfigurationException` / `TrtcUpstreamException` 映射为 `ApiResponse`（503/502）；可选：引入 `ApiException` 基类合并为一个 `@ExceptionHandler`
- [x] 2.4 确认**不**新增无区分的 `@ExceptionHandler(Exception.class)`（除非产品确认需要 500 兜底并更新 spec）

## 3. 鉴权 Filter 对齐

- [x] 3.1 `TrtcAuthFilter.writeUnauthorized` 改为输出 `ApiResponse.fail(401, detail)`，Content-Type `application/json`

## 4. 测试与文档

- [x] 4.1 更新 `WebApiFoundationIT`：400 断言改为 `$.code`、`$.message`、`$.data.errors`
- [x] 4.2 更新 TRTC 相关 IT：401 断言 `ApiResponse` 字段（若当前仅断言 status）
- [x] 4.3 更新 `README.md` 错误模型说明；同步 `trtc-start-response-envelope` 相关文档中「错误用 ProblemDetail」表述
- [x] 4.4 `mvn -q verify` 全绿
