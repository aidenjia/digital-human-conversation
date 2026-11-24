## Context

- 项目为 Spring Boot 3.5.x、Java 17，可执行 JAR 名为 `digital-human-0.0.1-SNAPSHOT.jar`，默认监听 `8080`，Actuator 暴露 `health`、`info`。
- 仓库无 Maven Wrapper，构建阶段需在镜像内安装 Maven，或使用外部构建再将 JAR 复制进镜像；本设计采用「镜像内 Maven 打包」以降低对宿主 Maven 的依赖。
- 你已明确要求基础镜像为 `openjdk:17-jdk-slim`（Docker Hub 上该系列已标记 deprecated，长期建议迁移 Eclipse Temurin；本变更按你的指定实现）。

## Goals / Non-Goals

**Goals:**

- 多阶段构建：最终运行镜像不包含源码与 Maven 缓存（构建阶段产物仅复制 JAR）。
- 非 root 用户运行 JVM。
- 提供基于 Actuator 的 `HEALTHCHECK`，与 `application.properties` 中 `management.endpoints.web.exposure.include` 一致。

**Non-Goals:**

- 不引入 Kubernetes Helm、Compose 编排文件（可按需另开变更）。
- 不修改应用业务代码或 API 契约。

## Decisions

1. **构建阶段基础镜像：`openjdk:17-jdk-slim`**  
   通过 `apt-get` 安装 `maven`，在 `/workspace` 下 `mvn -B -DskipTests package`。理由：与指定 JDK 版本一致，且无需在仓库中引入 `mvnw`。

2. **运行阶段基础镜像：同为 `openjdk:17-jdk-slim`**  
   理由：满足「基础镜像为 jdk-slim」的明确要求；若后续接受仅运行时 JRE，可改为 `openjdk:17-jre-slim` 以减小体积。

3. **健康检查：容器内 `wget` 请求 `/actuator/health`**  
   slim 镜像默认无 `curl`/`wget`，运行阶段单独安装 `wget`（轻量）用于 `HEALTHCHECK`。解析响应是否含 `UP` 与 Spring Boot 默认 health 文本兼容。

4. **JAR 路径写死为 `target/digital-human-0.0.1-SNAPSHOT.jar`**  
   与当前 `pom.xml` 中 `artifactId` + `version` 一致；版本变更时需同步 Dockerfile 或改为构建参数（非本变更范围）。

## Risks / Trade-offs

- **[Risk] `openjdk` 官方镜像维护策略** → 文档与 proposal 中说明迁移 Temurin 的路径；镜像标签失效时需更换 `FROM`。
- **[Risk] 构建耗时与网络** → 依赖 Maven Central；可通过构建缓存、公司 Nexus 镜像加速（非本仓库强制）。
- **[Trade-off] 运行阶段使用 JDK 而非 JRE** → 镜像大于 JRE 变体，但符合你对基础镜像的指定。

## Migration Plan

1. 本地验证：`docker build -t digital-human:local .` 后 `docker run --rm -p 8080:8080 digital-human:local`。
2. 流水线将镜像 tag 推送到内部仓库；回滚为部署上一镜像 tag。

## Open Questions

- 生产环境是否强制 `SPRING_PROFILES_ACTIVE=prod` 由编排注入即可，无需写死在 Dockerfile。
