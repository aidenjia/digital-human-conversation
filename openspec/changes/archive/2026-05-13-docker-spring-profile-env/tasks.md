## 1. OpenSpec 文案

- [x] 1.1 编写 `proposal.md`
- [x] 1.2 编写 `design.md`
- [x] 1.3 编写 `specs/docker-container-runtime/spec.md`（增量）
- [x] 1.4 编写本 `tasks.md`

## 2. 实现

- [x] 2.1 更新 `Dockerfile`：运行阶段注释说明 `SPRING_PROFILES_ACTIVE`（不写死非空默认）
- [x] 2.2 更新 `README.md`：`docker run` 示例含 `-e SPRING_PROFILES_ACTIVE=prod` 与多 profile 说明；文档化 `GET /internal/active-profiles`
- [x] 2.3 新增 `ActiveProfilesProbeController` 与 `ActiveProfilesProbeIT` / `ActiveProfilesProbeProdIT`、`WebApiFoundationIT` 前缀用例

## 3. 验证

- [x] 3.1 `openspec validate docker-spring-profile-env --strict`
