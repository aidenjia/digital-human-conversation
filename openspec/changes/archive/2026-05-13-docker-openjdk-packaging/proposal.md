## Why

需要以可重复的方式在容器内构建并运行本服务，便于本地联调与部署流水线；你已选定以 `openjdk:17-jdk-slim` 作为基础镜像，与项目 Java 17 一致。

## What Changes

- 在仓库根目录新增多阶段 `Dockerfile`：构建阶段在镜像内执行 Maven 打包，运行阶段仅携带可执行 JAR 与 JRE/JDK 运行时。
- 新增 `.dockerignore`，缩小构建上下文、避免把本地 `target/`、`.git` 等传入构建阶段。
- 运行阶段暴露 `8080`，可选 `HEALTHCHECK` 探测 `GET /actuator/health`（与业务 `/api/v1` 前缀无关）。

## Capabilities

### New Capabilities

- `docker-container-runtime`：定义容器镜像如何构建、启动与健康检查所应满足的行为。

### Modified Capabilities

- （无）本变更为交付物与运维侧能力，不改变既有 Web/API 规范中的业务要求。

## Impact

- 新增根目录 `Dockerfile`、`.dockerignore`；可选在 README 中补充 `docker build` / `docker run` 示例。
- CI/CD 或本地需具备 Docker；首次构建会拉取基础镜像并下载 Maven 依赖（网络敏感）。
