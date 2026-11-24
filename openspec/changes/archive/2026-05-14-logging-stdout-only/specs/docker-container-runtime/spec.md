## ADDED Requirements

### Requirement: 运行镜像不依赖应用日志文件目录或专用日志卷

交付的 `Dockerfile` SHALL NOT 为应用日志文件声明专用数据卷（例如 `VOLUME` 指向仅用于 Logback 本地文件的路径），SHALL NOT 将「必须挂载日志目录方可运行」作为默认运行前提。应用 SHALL 能够在未挂载日志卷、且无需可写应用日志目录的情况下启动并输出日志到标准输出。

#### Scenario: 无日志卷启动容器

- **WHEN** 使用 `docker run` 启动镜像且未挂载原 `/app/logs` 或等价仅用于文件日志的路径
- **THEN** 容器 SHALL 成功启动 JVM 进程，且应用 SHALL 通过标准输出产生可观测日志

#### Scenario: 审阅 Dockerfile 无强制日志卷

- **WHEN** 审阅仓库根目录运行阶段 `Dockerfile`
- **THEN** SHALL 不存在仅为应用滚动日志文件而声明的 `VOLUME`，且注释与 `README.md` 容器小节 SHALL 与「日志走标准输出、由引擎/平台采集」一致
