## 1. Dependencies & config

- [x] 1.1 pom：postgresql、mybatis-plus、h2、generator、freemarker
- [x] 1.2 `DataSourceConfig` / `MybatisPlusConfig` / mapper package-info

## 2. Profiles & test H2

- [x] 2.1 local/dev/test 数据源与 hikari / mybatis-plus
- [x] 2.2 单测 H2 配置
- [x] 2.3 CD `SPRING_DATASOURCE_USERNAME/PASSWORD`

## 3. Codegen（对齐 agent）

- [x] 3.1 `CodeGenerator`：Spotless 子进程 + `enableFileOverride`；去掉进程内 GJF
- [x] 3.2 pom 移除 google-java-format test 依赖；更新 `tools/codegen/README.md`
- [x] 3.3 冒烟生成后删除 `DemoUser*`

## 4. Docs & OpenSpec

- [x] 4.1 README / 文档反映接库能力
- [x] 4.2 补 OpenSpec 并同步主规格、归档
