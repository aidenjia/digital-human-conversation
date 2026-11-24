## Why

当前仓库已具备 Spring Boot Web、Actuator、Logback 与基础端口配置，但缺少典型 **生产级 Web 服务初始化** 能力（校验、统一错误语义、优雅停机、可观测与文档化约定等），README 也未系统说明「还应配置什么」。需要在不扩大为多模块的前提下，补齐骨架并沉淀为可验收的需求与任务。

## What Changes

- 引入 **Bean Validation**（`spring-boot-starter-validation`）及最小全局异常处理，使 API 入参错误返回一致、可读。
- 在 `application.properties` 中补充 **Web/Tomcat 常见基线**：UTF-8 编码、优雅停机、可选压缩；Jackson 时区与序列化约定（便于前后端与日志对齐）。
- 启用 **RFC 9457 Problem Details**（`spring.mvc.problemdetails.enabled=true`），与异常处理风格一致。
- 增加 **根路径可读响应**（如 `GET /` 返回服务名与版本占位），便于负载均衡与人工探活。
- **README** 重组为「技术栈 → 快速开始 → 配置矩阵 → Web/API 约定 → 日志 → 初始化清单 → 结构」，并列出可选后续项（CORS、OpenAPI、安全、限流等）。

## Capabilities

### New Capabilities

- `web-api-foundation`：定义本服务作为 HTTP API 时的基线行为（校验失败响应、未处理异常、Problem Details、根路径探活、编码与停机相关配置的可验收要求）。

### Modified Capabilities

- （无）仓库内尚无已发布的 `openspec/specs/` 能力文档。

## Impact

- **依赖**：新增 `spring-boot-starter-validation`（随父 BOM 管理版本）。
- **代码**：新增 `com.jupiter.digital.human` 包下少量配置类/异常类（保持最小）。
- **配置**：`application.properties`、可选 `application-prod.properties` 增补键；不改变现有 Actuator 与 Logback 文件策略的主旨。
- **文档**：`README.md` 与 OpenSpec 变更目录内 `proposal` / `design` / `specs` / `tasks`。
