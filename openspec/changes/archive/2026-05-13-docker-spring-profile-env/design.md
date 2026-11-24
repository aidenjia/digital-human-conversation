## Context

Spring Boot 将环境变量 **`SPRING_PROFILES_ACTIVE`** 绑定到 `spring.profiles.active`（ relaxed binding）。容器内亦支持 `JAVA_OPTS` 传入 `-Dspring.profiles.active=prod`，但与 Docker / Compose / K8s 的 `env` 块相比，统一用 **`SPRING_PROFILES_ACTIVE`** 更清晰。

## Goals / Non-Goals

**Goals:**

- 文档与镜像层注释明确推荐变量名与示例。
- 不在镜像中写死 `ENV SPRING_PROFILES_ACTIVE=prod`，避免本地镜像误带生产 profile。

**Non-Goals:**

- 不新增 `application-docker.properties` 或改 Logback profile 逻辑（已有 `springProfile` 区分）。

## Decisions

1. **仅文档 + Dockerfile 注释**：不在 Dockerfile 设置非空默认 `SPRING_PROFILES_ACTIVE`，部署方通过 `docker run -e` / Compose / K8s `env` 注入。

2. **备选 `JAVA_OPTS`**：在 README 一行说明也可用 `-Dspring.profiles.active=prod`（与现有 `JAVA_OPTS` 入口一致）。

## Risks / Trade-offs

- **[Risk]** 用户误写 `SPRING_PROFILE_ACTIVE`（少 s）→ 在 README 写清全名。

## Migration Plan

- 无数据迁移；已有 `docker run` 脚本可逐步加上 `-e`。

## Open Questions

- 无。
