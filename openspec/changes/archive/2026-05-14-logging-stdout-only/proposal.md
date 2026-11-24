## Why

当前应用通过 Logback 将日志写入滚动文件（全量与 ERROR 分离），`prod` 环境仅写文件不写控制台。在容器与平台化部署中，标准输出/标准错误更利于由编排与日志采集管道统一收集与检索；文件落盘还带来卷挂载、权限与磁盘配额等运维成本。将日志统一改为标准输出，可简化部署并与常见云原生实践对齐。

## What Changes

- 调整 `logback-spring.xml`：根日志器仅绑定控制台 appender，移除（或停用）`RollingFileAppender` 及按 profile 区分的「仅文件」策略，使所有激活的 Spring profile 下日志均输出到标准输出。
- 视情况收紧或删除 `application.properties` 中与文件路径相关的 `logging.file.path` / `LOG_PATH` 默认说明（若不再使用文件 appender，则避免误导运维仍挂载日志卷）。
- 更新 `Dockerfile`：去掉仅为文件日志准备的 `logs/archive` 目录、`chown` 与 `VOLUME /app/logs`（或改为仅保留与 stdout 无关的必要步骤）。
- 更新 `README.md`（及任何引用「日志目录」「日志持久化卷」的文档）：说明日志走 stdout/stderr，Docker 示例不再要求 `-v ...:/app/logs`。

## Capabilities

### New Capabilities

- `application-logging`: 规定应用运行时的日志输出目标为进程标准输出（及错误输出策略与现有级别约定一致），不要求将业务日志持久化到应用镜像内或挂载卷上的本地日志文件。

### Modified Capabilities

- `docker-container-runtime`: 运行镜像不再以「可写日志目录 + 声明日志卷」为交付前提；容器默认行为与文档 SHALL 与仅 stdout 日志一致（例如移除对 `/app/logs` 卷与目录的硬性依赖说明）。

## Impact

- 受影响文件：`src/main/resources/logback-spring.xml`、`src/main/resources/application.properties`（可选）、`Dockerfile`、`README.md`。
- **BREAKING**：依赖宿主机或命名卷收集 `logs/*.log` 文件的运维流程需改为采集容器 stdout/stderr；若外部系统曾解析文件路径或归档 gzip，需迁移到平台日志方案。
