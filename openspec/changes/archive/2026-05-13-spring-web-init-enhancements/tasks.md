## 1. 依赖与构建元数据

- [x] 1.1 在 `pom.xml` 增加 `spring-boot-starter-validation`
- [x] 1.2 配置 `spring-boot-maven-plugin` 的 `build-info` 目标以生成版本元数据

## 2. 应用配置

- [x] 2.1 在 `application.properties` 增加 UTF-8 编码、优雅停机、压缩、Jackson 时区、Problem Details 等基线项
- [x] 2.2 视需要在 `application-prod.properties` 收紧或覆盖与 Web 相关的项

## 3. Web 与异常处理

- [x] 3.1 实现 `@RestControllerAdvice`：校验异常 → 400 ProblemDetail；未处理异常 → 500 泛化 ProblemDetail
- [x] 3.2 实现 `GET /` 返回 `name`、`version`（优先 `BuildProperties`）
- [x] 3.3 提供最小 `@Valid` 示例接口并补充 `MockMvc` / 集成测试覆盖根路径与校验失败

## 4. 文档

- [x] 4.1 优化 `README.md`：技术栈、配置矩阵、初始化清单、后续可选能力、与 OpenSpec 变更的对应关系
