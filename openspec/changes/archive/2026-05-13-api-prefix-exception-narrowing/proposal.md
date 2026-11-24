## Why

业务接口路径在控制器上硬编码 `/api/v1`，与「统一前缀」的演进不一致；全局 `@ExceptionHandler(Exception.class)` 会吞掉所有未分类异常，易与 Spring MVC 自带错误解析（含 Problem Details）冲突，且难以区分「预期内的框架异常」与「真正的缺陷」。

## What Changes

- 通过 **集中式路径前缀**（`PathMatchConfigurer#addPathPrefix`）将 `com.jupiter.digital.human.api` 包下 `@RestController` 统一映射到 **`/api/v1`**，控制器上只保留资源路径（如 `/echo`）。
- **根探活 `GET /`、Actuator** 等仍保持无前缀，不受该策略影响。
- `GlobalExceptionHandler` **继承 `ResponseEntityExceptionHandler`**，用 **`@Override` 定制校验类异常**；**删除对 `Exception` / `Throwable` 的兜底**；保留 **`ConstraintViolationException`** 等 MVC 层不易由父类覆盖的声明式校验异常。
- 更新集成测试与 README 中的路径与错误处理说明。

## Capabilities

### New Capabilities

- `api-routing-policy`：定义业务 API 包级路径前缀与异常处理边界（禁止宽泛的 Exception 兜底）。

### Modified Capabilities

- （无）主分支 `openspec/specs/` 尚未归档上一变更；本变更以新增能力描述为主。

## Impact

- 代码：`WebMvcConfiguration`（或等价配置类）、`EchoController` 映射调整、`GlobalExceptionHandler` 重构。
- 文档：README、本变更下 `tasks.md`。
- 行为：对外 URL 仍为 `POST /api/v1/echo` 时，**实现方式**从「注解写死前缀」改为「前缀由配置统一注入」；未捕获的业务异常将更多依赖 **Spring Boot 默认错误链路**（与 `spring.mvc.problemdetails` 一致）。
