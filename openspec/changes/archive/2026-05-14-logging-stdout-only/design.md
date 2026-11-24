## Context

项目为 Spring Boot 应用，当前通过 `logback-spring.xml` 配置 `RollingFileAppender`（全量与 ERROR），非 `prod` 时额外输出控制台，`prod` 仅写文件。`Dockerfile` 创建 `logs/archive`、声明 `VOLUME /app/logs`，`README` 建议挂载命名卷持久化日志。目标环境包含本地开发与容器/K8s 等，平台侧普遍通过 stdout/stderr 采集日志。

## Goals / Non-Goals

**Goals:**

- 所有 profile 下业务与框架日志统一输出到标准输出（沿用 Spring Boot 默认控制台编码与 Logback `CONSOLE` appender 模式即可）。
- 移除对本地滚动日志文件的依赖，简化镜像层与挂载说明。
- 保持现有日志级别策略的可配置性（如 `application*.properties` 中的 `logging.level.*`），不因改输出目标而削弱可观测性。

**Non-Goals:**

- 不改变业务 API、Actuator 暴露范围或健康检查语义。
- 不引入新的日志后端（如直接写远端、边车）——仅改为 stdout；平台侧采集由运维/编排负责。
- 不要求在本变更中实现结构化 JSON 日志（若后续需要可另起变更）。

## Decisions

1. **Logback：根 logger 仅引用 `CONSOLE`**  
   删除 `FILE_ALL`、`FILE_ERROR` 及相关 `springProfile` 分支中对文件的引用；保留 `include` 的 `console-appender.xml` 与业务包 logger 级别配置。理由：单一输出面，避免 prod 与 dev 行为分叉。

2. **不再使用 `logging.file.path` 驱动 Logback 文件路径**  
   从 `logback-spring.xml` 移除对 `LOG_PATH`/`logging.file.path` 的 `springProperty`（若已无引用）。`application.properties` 中可删除 `logging.file.path` 或保留注释说明「已废弃，仅历史兼容」——设计倾向删除以免误导。理由：减少无效配置与文档漂移。

3. **Dockerfile：去掉日志目录与 `VOLUME`**  
   删除 `mkdir -p logs/archive`、`chown` 日志目录及 `VOLUME ["/app/logs"]`；保留 `WORKDIR`、`USER app`、`JAR` 权限。理由：stdout 不需要可写日志目录；匿名卷反而造成「以为有持久化」的误解。

4. **README：容器示例去掉 `-v ...:/app/logs`**  
   增加一句说明：日志请通过容器引擎采集标准输出。理由：与交付物一致。

## Risks / Trade-offs

- **[Risk] 运维仍按文件路径采集** → 在 proposal/README 标明 **BREAKING**，迁移到 stdout 采集或 sidecar。  
- **[Risk] 大流量下控制台 I/O 与性能** → 一般低于磁盘滚动写；若成为瓶颈由平台侧限流或后续引入异步 appender 评估。  
- **[Risk] 多行异常栈在部分采集器中的合并** → 属平台能力；应用侧保持现有 `%wEx` 等模式在控制台 appender 中即可。

## Migration Plan

1. 合并后新镜像不再创建 `/app/logs` 卷；编排删除对 `/app/logs` 的 volumeMount（若存在）。  
2. 在预发/生产切换采集配置：从「读文件」改为「docker logs」/容器 stdout 或对应 CRI 日志路径。  
3. 回滚：恢复上一版本镜像与 `logback-spring.xml` 文件 appender（保留 git 历史即可）。

## Open Questions

- 无。若团队强制要求 prod 仍双写「文件+控制台」，可作为非目标外的例外单独讨论，本设计不采纳。
