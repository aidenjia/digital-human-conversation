# docker-container-runtime Specification

## Purpose
TBD - created by archiving change docker-openjdk-packaging. Update Purpose after archive.
## Requirements
### Requirement: 镜像为 Java 21 JDK 且默认经国内可拉取的前缀

交付的 `Dockerfile` SHALL 通过 `ARG BASE_IMAGE` 指定构建与运行两阶段的同一 JDK 21 基础镜像，以匹配项目 `java.version` 21。默认 `BASE_IMAGE` SHALL 使用 DaoCloud 文档推荐前缀 `m.daocloud.io/docker.io/library/` 下的 **`eclipse-temurin:21-jdk-jammy`**（Docker Hub 官方维护，国内拉取通常不受 `library/openjdk` 弃用镜像的 403 限制）。若需使用 `openjdk:21-jdk-slim` 等其他镜像， SHALL 通过 `docker build --build-arg BASE_IMAGE=...` 指定已验证可拉取的完整路径。

#### Scenario: 默认 BASE_IMAGE

- **WHEN** 未传入 `--build-arg BASE_IMAGE`
- **THEN** 默认值 SHALL 为 `m.daocloud.io/docker.io/library/eclipse-temurin:21-jdk-jammy`

#### Scenario: 审阅 Dockerfile 中的基础镜像

- **WHEN** 审阅仓库根目录 `Dockerfile` 中的 `ARG BASE_IMAGE` 与两处 `FROM ${BASE_IMAGE}`
- **THEN** 两阶段使用同一解析后的 JDK 21 镜像引用；默认引用 SHALL 包含 `eclipse-temurin` 与 `21-jdk`

### Requirement: 多阶段构建产出可执行 JAR

构建流程 SHALL 在容器内完成 Maven 打包，并将生成的 Spring Boot 可执行 JAR 复制到最终镜像；最终镜像 SHALL 不包含项目源码目录（仅必要文件与 JAR）。

#### Scenario: 构建成功且入口为 java -jar

- **WHEN** 在含源码的仓库根目录执行 `docker build`
- **THEN** 构建完成且无将 `src/` 原样复制到最终运行层的必要；容器默认命令 SHALL 以 `java` 执行该 JAR（可通过环境变量扩展 JVM 参数）

### Requirement: 默认监听与健康检查

运行容器 SHALL 对外暴露应用配置的 HTTP 端口（默认 8080）。`Dockerfile` SHOULD 提供 `HEALTHCHECK`，对 `http://127.0.0.1:8080/actuator/health` 发起探测，并在健康未就绪时返回非零退出码。

#### Scenario: 健康端点可访问时检查通过

- **WHEN** 应用已启动且 Actuator 暴露 `health` 端点
- **THEN** `HEALTHCHECK` 使用的探测命令在进程健康时应成功退出

### Requirement: Prometheus metrics share the application HTTP port

The running container SHALL expose Prometheus metrics on the same configured
HTTP port as the business API and health probes (default `8080`). The system
SHALL NOT require a separate management port for `/actuator/prometheus` in the
default runtime configuration covered by this capability.

#### Scenario: Metrics reachable on the published container port

- **WHEN** the container listens on the published application port (default 8080)
  and Actuator exposes `prometheus`
- **THEN** `GET /actuator/prometheus` on that port succeeds without requiring a
  second published management port

### Requirement: 非 root 运行

最终运行镜像中的 JVM 进程 SHALL 不以 root 用户运行。

#### Scenario: 进程用户非 root

- **WHEN** 容器启动且业务进程已创建
- **THEN** 该进程的有效用户 SHALL 不是 root（例如专用系统用户）

### Requirement: 容器启动可通过环境变量配置 Spring Profile

交付物 SHALL 在根目录 `Dockerfile`（运行阶段注释）与 `README.md`（容器运行小节）中说明：使用 `docker run`（或等效编排）启动镜像时，可通过环境变量 **`SPRING_PROFILES_ACTIVE`** 设置 Spring 的 **`spring.profiles.active`**。`Dockerfile` SHALL **不得**将 `SPRING_PROFILES_ACTIVE` 设为非空默认值，以免覆盖编排侧注入。

#### Scenario: 指定单 profile 为 prod

- **WHEN** 使用 `docker run` 且传入 `-e SPRING_PROFILES_ACTIVE=prod`
- **THEN** 应用 SHALL 激活 `prod` profile（例如加载 `application-prod.properties`）

#### Scenario: 指定多 profile

- **WHEN** 传入 `-e SPRING_PROFILES_ACTIVE=prod,metrics`（示例，多个 profile 以逗号分隔）
- **THEN** 应用 SHALL 同时激活所列 profile（顺序与合并规则遵循 Spring Boot 既有行为）

### Requirement: Profile verification uses startup diagnostics

部署 SHALL 通过启动日志和实际配置验证 `SPRING_PROFILES_ACTIVE`，不新增公开的运行环境诊断 API。基础 HTTP 探针为根 GET 与 Actuator health。

#### Scenario: Retained probe endpoints

- **WHEN** 运维检查容器健康与标识
- **THEN** 使用 `GET /` 和 `GET /actuator/health`，不依赖已不存在的 internal profile 接口

### Requirement: 运行镜像不依赖应用日志文件目录或专用日志卷

交付的 `Dockerfile` SHALL NOT 为应用日志文件声明专用数据卷（例如 `VOLUME` 指向仅用于 Logback 本地文件的路径），SHALL NOT 将「必须挂载日志目录方可运行」作为默认运行前提。应用 SHALL 能够在未挂载日志卷、且无需可写应用日志目录的情况下启动并输出日志到标准输出。

#### Scenario: 无日志卷启动容器

- **WHEN** 使用 `docker run` 启动镜像且未挂载原 `/app/logs` 或等价仅用于文件日志的路径
- **THEN** 容器 SHALL 成功启动 JVM 进程，且应用 SHALL 通过标准输出产生可观测日志

#### Scenario: 审阅 Dockerfile 无强制日志卷

- **WHEN** 审阅仓库根目录运行阶段 `Dockerfile`
- **THEN** SHALL 不存在仅为应用滚动日志文件而声明的 `VOLUME`，且注释与 `README.md` 容器小节 SHALL 与「日志走标准输出、由引擎/平台采集」一致

