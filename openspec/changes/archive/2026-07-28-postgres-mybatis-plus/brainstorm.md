## Design Summary

一期为 `digital-human-conversation` 接入 Postgres + HikariCP + MyBatis-Plus：手写唯一 `HikariDataSource`（启动建池失败则进程失败）、全局主键 `ASSIGN_ID`、分环境 JDBC、开发期 codegen。与 agent 共用 `digital_human_dev` / `digital_human_test`。一期**不含业务 CRUD**。

Codegen 已与 agent 对齐：生成后 `mvn spotless:apply`，支持 `enableFileOverride`，IDEA **无需** `--add-exports`。

本 change 为**事后补档**：接库与 codegen 对齐已完成，OpenSpec 用于沉淀主规格并归档。

## Alternatives Considered

### Alternative A: Spring Boot DataSource 自动配置
- **Approach**: 仅靠 starter 自动装配 Hikari
- **Pros**: 代码少
- **Cons**: 启动失败语义与 `initialization-fail-timeout` 不易统一
- **Why not chosen**: 选定方式 C 手写建池

### Alternative B: 手写 HikariDataSource + MyBatis-Plus
- **Approach**: `DataSourceConfig` + `MybatisPlusConfig` + 分环境 yml + codegen
- **Pros**: 启动连不上即失败；主键约定清晰；可复用到 agent
- **Cons**: 多配置类
- **Why chosen**: 一期接库标准方案

### Alternative C: Codegen 进程内 google-java-format
- **Approach**: 生成后调用 GJF + `RemoveUnusedImports`
- **Pros**: 不启 Maven 子进程
- **Cons**: JDK 21 需 `--add-exports`
- **Why not chosen**: 已改为与 agent 同款 `mvn spotless:apply`

## Agreed Approach

采用 **Alternative B** 接库；codegen 格式化采用 Spotless 子进程（非 Alternative C）。

## Key Decisions

1. 共享库：`digital_human_dev` / `digital_human_test`；本地 `127.0.0.1:35432`；集群 pgpool
2. 池名 `conversation-hikari`；`maximum-pool-size: 10`
3. 主键全局 `ASSIGN_ID`
4. 一期无业务 CRUD；codegen 不生成 Controller
5. CD：`SPRING_DATASOURCE_USERNAME` / `PASSWORD`
6. 单测 H2（`src/test/resources`）
7. Codegen：`CODEGEN_*` + Spotless；默认覆盖已有生成文件

## Open Questions

无。
