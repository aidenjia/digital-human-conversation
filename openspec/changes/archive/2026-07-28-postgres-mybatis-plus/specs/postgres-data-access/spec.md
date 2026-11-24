# postgres-data-access Specification

## Purpose

`digital-human-conversation` 通过 HikariCP + MyBatis-Plus 访问与 agent 共享的 Postgres。一期仅接库与开发工具链，不含业务 CRUD API。Codegen 格式化与 CI Spotless 对齐。

## ADDED Requirements

### Requirement: Application MUST expose a single Hikari DataSource that fails fast on connect errors

Conversation MUST 提供唯一手写 `HikariDataSource` Bean（`DataSourceConfig`）：从 `spring.datasource.url/username/password`（及可选 driver）装配，并将 `spring.datasource.hikari.*` 绑定到 `HikariConfig`。构造 `new HikariDataSource(config)` 时 MUST 建池；在 `initialization-fail-timeout` 内无法获得连接时 MUST 导致应用启动失败。池名 MUST 为 `conversation-hikari`（单测 H2 可使用独立池名）。

#### Scenario: Successful pool start
- **WHEN** JDBC 可达且凭据有效
- **THEN** 启动日志 MUST 出现 `conversation-hikari`（或测试池名）的 Start completed，并 MUST 能获得 Postgres 或测试 H2 连接

#### Scenario: Unreachable database
- **WHEN** 启动时数据库不可达或密码为空导致无法建池
- **THEN** 应用 MUST 启动失败

---

### Requirement: MyBatis-Plus MUST use ASSIGN_ID and scan persistence mappers

`MybatisPlusConfig` MUST `@MapperScan` `con.aidenjia.digitalhuman.conversation.infrastructure.persistence.mapper`，且全局 `IdType` MUST 为 `ASSIGN_ID`。配置与 yml `mybatis-plus.global-config.db-config.id-type: assign_id` MUST 一致。业务主键 MUST NOT 依赖数据库自增。

#### Scenario: Mapper package is scanned
- **WHEN** 在约定 mapper 包下存在 Mapper 接口
- **THEN** Spring 容器 MUST 将其注册为 Bean

#### Scenario: Primary key generation
- **WHEN** 通过 MyBatis-Plus 插入未手动指定 id 的实体
- **THEN** 主键 MUST 由应用侧雪花（`ASSIGN_ID`）生成

---

### Requirement: Environment-specific JDBC MUST target the shared digital_human databases

- **local**：可达的共享开发库（`application-local.yml` 可不入库）
- **dev**：`jdbc:postgresql://pg.example.test:5432/digital_human_dev`
- **test**（集群 profile）：同 host，库名 `digital_human_test`
- 单元/IT：`src/test/resources` MUST 提供 H2（PostgreSQL 兼容模式）

集群部署 MUST 可通过 `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` 覆盖凭据。

#### Scenario: Dev profile uses shared dev database
- **WHEN** 激活 `dev` profile
- **THEN** JDBC URL MUST 指向共享 `digital_human_dev`

#### Scenario: Unit tests use in-memory H2
- **WHEN** 运行依赖 Spring 上下文的测试且使用 test 资源中的 H2 数据源配置
- **THEN** DataSource MUST 不依赖集群 Postgres

---

### Requirement: Phase-1 MUST NOT ship business CRUD APIs

一期 MUST NOT 将临时冒烟 `DemoUser*` 等作为对外正式契约保留。Codegen MUST 默认不生成 Controller；业务 API 按需手写。

#### Scenario: No demo CRUD after smoke
- **WHEN** 接库/codegen 验证完成并准备合入
- **THEN** 仓库 MUST NOT 保留仅用于冒烟的 `DemoUser*` 持久化产物（文档表名示例除外）

---

### Requirement: Dev-time codegen MUST format with Spotless

`CodeGenerator` MUST 基于 `CODEGEN_DB_URL` / `CODEGEN_DB_USER` / `CODEGEN_DB_PASSWORD` / `CODEGEN_TABLES` 生成 Entity、Mapper、XML、Service、ServiceImpl。生成结束后 MUST 执行 `mvn spotless:apply`（失败则中止）。MUST NOT 将进程内 google-java-format 作为默认路径。重复生成 MUST 允许覆盖已有 Entity/Mapper/Service（`enableFileOverride`）。

#### Scenario: Generate then format without VM exports
- **WHEN** 开发者仅配置 CODEGEN_*（无 `--add-exports`）并运行 CodeGenerator
- **THEN** 源码 MUST 生成到 persistence 包与 `src/main/resources/mapper`，且 Spotless 步骤 MUST 成功或显式失败
