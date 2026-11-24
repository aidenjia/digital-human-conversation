## ADDED Requirements

### Requirement: 镜像为 Java 17 JDK 且默认经国内可拉取的前缀

交付的 `Dockerfile` SHALL 通过 `ARG BASE_IMAGE` 指定构建与运行两阶段的同一 JDK 17 基础镜像，以匹配项目 `java.version` 17。默认 `BASE_IMAGE` SHALL 使用 DaoCloud 文档推荐前缀 `m.daocloud.io/docker.io/library/` 下的 **`eclipse-temurin:17-jdk-jammy`**（Docker Hub 官方维护，国内拉取通常不受 `library/openjdk` 弃用镜像的 403 限制）。若需使用 `openjdk:17-jdk-slim` 等其他镜像， SHALL 通过 `docker build --build-arg BASE_IMAGE=...` 指定已验证可拉取的完整路径。

#### Scenario: 默认 BASE_IMAGE

- **WHEN** 未传入 `--build-arg BASE_IMAGE`
- **THEN** 默认值 SHALL 为 `m.daocloud.io/docker.io/library/eclipse-temurin:17-jdk-jammy`

#### Scenario: 审阅 Dockerfile 中的基础镜像

- **WHEN** 审阅仓库根目录 `Dockerfile` 中的 `ARG BASE_IMAGE` 与两处 `FROM ${BASE_IMAGE}`
- **THEN** 两阶段使用同一解析后的 JDK 17 镜像引用；默认引用 SHALL 包含 `eclipse-temurin` 与 `17-jdk`

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

### Requirement: 非 root 运行

最终运行镜像中的 JVM 进程 SHALL 不以 root 用户运行。

#### Scenario: 进程用户非 root

- **WHEN** 容器启动且业务进程已创建
- **THEN** 该进程的有效用户 SHALL 不是 root（例如专用系统用户）
