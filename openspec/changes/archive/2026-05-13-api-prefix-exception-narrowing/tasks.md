## 1. 路由前缀

- [x] 1.1 新增 `WebMvcConfigurer` 实现，对 `com.jupiter.digital.human.api` 包注册 `/api/v1` 前缀
- [x] 1.2 调整 `EchoController`：去掉类上重复的 `/api/v1`，仅保留资源路径

## 2. 异常处理收窄

- [x] 2.1 将 `GlobalExceptionHandler` 改为继承 `ResponseEntityExceptionHandler`，用 `@Override` 处理 `MethodArgumentNotValidException`
- [x] 2.2 删除对 `Exception` / `Throwable` 的 `@ExceptionHandler`；保留 `ConstraintViolationException` 等窄化处理器

## 3. 验证与文档

- [x] 3.1 更新 `WebApiFoundationIT` 与 `mvn verify`
- [x] 3.2 更新 `README.md` 中 API 前缀与异常策略说明
