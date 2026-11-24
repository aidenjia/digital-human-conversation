## Why

Conversation 需具备关系库访问能力，支撑后续业务表；一期先接库与工具链，并与 agent 同构（含 codegen Spotless）。

## What Changes

**Postgres + Hikari + MyBatis-Plus**
- From: 无关系库
- To: 手写 Hikari；MyBatis-Plus；`ASSIGN_ID`；分环境 JDBC；CD 凭据注入
- Impact: non-breaking（无既有 DB API）；启动依赖可达 Postgres（或测试 H2）

**Codegen**
- From: 进程内 GJF（需 `--add-exports`）
- To: 生成后 `mvn spotless:apply`；`enableFileOverride`；去掉 test 依赖 `google-java-format`
- Impact: 仅开发期；IDEA 无需 VM exports

## Capabilities

### New Capabilities

- `postgres-data-access`: Conversation 对共享 Postgres 的连接池、MyBatis-Plus 约定、环境配置、codegen（Spotless）、CD 注入与一期范围

### Modified Capabilities

- （无）

## Impact

- 代码：`infrastructure.persistence.config`、yml、pom、`tools/codegen`、README
- CD：`digital-human-conversation/deployment.yaml` 的 `SPRING_DATASOURCE_*`
- API：一期无新正式 HTTP 契约
