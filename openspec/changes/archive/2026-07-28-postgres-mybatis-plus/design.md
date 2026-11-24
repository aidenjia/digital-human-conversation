## Context

`digital-human-conversation` 原先无关系库。业务要求接入 Postgres（Hikari + MyBatis-Plus），一期只接库、与 agent 共用库，主键由应用生成。后续将 codegen 格式化从进程内 GJF 对齐为 agent 的 Spotless 方案。

## Goals / Non-Goals

**Goals**

- 启动建立 Hikari 连接池；连不上则启动失败
- MyBatis-Plus 扫描 `infrastructure.persistence.mapper`；默认 `ASSIGN_ID`
- local / dev / test 与单测 H2
- codegen：Entity/Mapper/XML/Service；`mvn spotless:apply`；不生成 Controller
- CD 注入数据源凭据

**Non-Goals**

- 业务表与正式 CRUD（冒烟后删除）
- 分库分表、多数据源
- ConvoAI 业务逻辑变更

## Decisions

| 决策 | 选择 | 理由 |
|------|------|------|
| DataSource | 手写 `HikariDataSource` | 方式 C，启动失败可见 |
| ORM | MyBatis-Plus 3.5.12 | 团队选型 |
| 主键 | `ASSIGN_ID` | 禁止库自增 |
| 库 | 共享 `digital_human_*` | 干系人确认 |
| Codegen 格式化 | `mvn spotless:apply` | 与 agent / CI 一致，免 `--add-exports` |

## Risks / Trade-offs

- 集群缺 `SPRING_DATASOURCE_PASSWORD` 可能导致空密码启动失败（CD 已注入缓解）
- 共享库连接数需与 agent 合计监控
- Codegen 覆盖已有文件：重复生成会覆盖，需知悉

## Migration Plan

- 无数据迁移；无破坏性 API
- 部署：CD 注入 env 后发带 DataSource 的镜像

## Open Questions

无。
