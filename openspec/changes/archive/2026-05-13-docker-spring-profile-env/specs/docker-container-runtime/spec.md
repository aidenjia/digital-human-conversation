## ADDED Requirements

### Requirement: 容器启动可通过环境变量配置 Spring Profile

交付物 SHALL 在根目录 `Dockerfile`（运行阶段注释）与 `README.md`（容器运行小节）中说明：使用 `docker run`（或等效编排）启动镜像时，可通过环境变量 **`SPRING_PROFILES_ACTIVE`** 设置 Spring 的 **`spring.profiles.active`**。`Dockerfile` SHALL **不得**将 `SPRING_PROFILES_ACTIVE` 设为非空默认值，以免覆盖编排侧注入。

#### Scenario: 指定单 profile 为 prod

- **WHEN** 使用 `docker run` 且传入 `-e SPRING_PROFILES_ACTIVE=prod`
- **THEN** 应用 SHALL 激活 `prod` profile（例如加载 `application-prod.properties`）

#### Scenario: 指定多 profile

- **WHEN** 传入 `-e SPRING_PROFILES_ACTIVE=prod,metrics`（示例，多个 profile 以逗号分隔）
- **THEN** 应用 SHALL 同时激活所列 profile（顺序与合并规则遵循 Spring Boot 既有行为）

### Requirement: 提供可验证激活 profile 的只读 HTTP 接口

应用 SHALL 暴露 **`GET /internal/active-profiles`**（不经 `/api/v1` 业务前缀），返回 JSON，且 SHALL 包含键 **`activeProfiles`** 与 **`defaultProfiles`**，二者均为字符串数组，内容分别来自 Spring `Environment#getActiveProfiles()` 与 `getDefaultProfiles()`，以便在容器或本地验证 `SPRING_PROFILES_ACTIVE` 等配置是否生效。

#### Scenario: 访问探针接口

- **WHEN** 客户端请求 `GET /internal/active-profiles`
- **THEN** 响应状态码 SHALL 为 200，且 JSON 中 SHALL 存在数组类型的 `activeProfiles` 与 `defaultProfiles`

#### Scenario: 探针不经业务 API 前缀

- **WHEN** 客户端请求 `GET /api/v1/internal/active-profiles`
- **THEN** 响应 SHALL 为 404（无控制器映射该路径）
