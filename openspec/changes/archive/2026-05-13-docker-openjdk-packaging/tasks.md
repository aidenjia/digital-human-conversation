## 1. OpenSpec 与交付物

- [x] 1.1 编写 `proposal.md`（动机、范围、能力）
- [x] 1.2 编写 `design.md`（多阶段、健康检查、用户）
- [x] 1.3 编写 `specs/docker-container-runtime/spec.md`
- [x] 1.4 编写本 `tasks.md`

## 2. 实现

- [x] 2.1 在仓库根目录新增 `Dockerfile`（多阶段 JDK 17、默认 Temurin + DaoCloud 前缀、非 root、`HEALTHCHECK`、`VOLUME /app/logs`）
- [x] 2.2 新增 `.dockerignore` 排除 `target/`、`.git` 等

## 3. 验证

- [x] 3.1 本地执行 `docker build -t digital-human:local .`（需本机 Docker 与网络）
