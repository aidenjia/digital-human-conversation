## Why

使用 Docker 部署时，需要在不重建镜像的前提下切换运行环境（如 `dev` / `prod`）。Spring Boot 支持通过环境变量绑定 `spring.profiles.active`，但仓库需在交付物与规格中**显式约定**变量名与用法，避免误用 JVM 系统属性或错误变量名。

## What Changes

- 根目录 `Dockerfile`：在运行阶段补充 **`SPRING_PROFILES_ACTIVE`** 的说明注释（可选保留空 `ENV` 仅占位，避免写死非空 profile）。
- `README.md`：在容器运行说明中增加 **`docker run -e SPRING_PROFILES_ACTIVE=...`** 与多 profile 示例；说明用 **`GET /internal/active-profiles`** 验证生效。
- 新增 **`GET /internal/active-profiles`** 只读接口（`web` 包）及集成测试，用于确认当前激活的 Spring Profile。

## Capabilities

### New Capabilities

- （无）

### Modified Capabilities

- `docker-container-runtime`：增加「容器启动可通过环境变量配置 Spring Profile」的要求。

## Impact

- 仅文档与 `Dockerfile` 注释；不改变默认应用行为；无需改 `application*.properties` 或 Java 代码。
