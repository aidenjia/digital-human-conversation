## ADDED Requirements

### Requirement: 应用日志输出至标准输出

在任意激活的 Spring profile 下，应用运行时的常规日志（含由根 logger 接管的框架与业务日志）SHALL 输出到进程的标准输出，SHALL NOT 依赖 Logback `RollingFileAppender` 或等价机制将同等级别的日志作为主路径写入容器内或挂载卷上的本地日志文件。

#### Scenario: 默认与本地 profile 启动

- **WHEN** 使用默认配置或显式非 `prod` profile（例如未设置 `spring.profiles.active` 或 `dev`）启动应用
- **THEN** 日志 SHALL 出现在标准输出中，且 SHALL 不存在因缺少可写日志目录而导致启动失败的主路径写文件需求

#### Scenario: 生产 profile 启动

- **WHEN** 使用 `spring.profiles.active=prod`（或等效环境变量）启动应用
- **THEN** 日志 SHALL 同样输出到标准输出，SHALL NOT 仅写入文件而不输出到控制台

### Requirement: 日志级别仍可通过 Spring 配置覆盖

日志输出目标为控制台后，应用 SHALL 继续支持通过 `application.properties`、`application-*.properties` 或环境变量等方式配置 `logging.level.*`（含根级别与包级别），行为遵循 Spring Boot 既有约定。

#### Scenario: 调整业务包级别

- **WHEN** 在配置中将 `con.aidenjia.digitalhuman` 的日志级别设为 `INFO`
- **THEN** 该包下日志 SHALL 按该级别过滤后仍从标准输出可见（不低于所设级别的条目）
