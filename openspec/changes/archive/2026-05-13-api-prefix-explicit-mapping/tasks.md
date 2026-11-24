## 1. OpenSpec

- [x] 1.1 `proposal.md`、`design.md`
- [x] 1.2 `specs/api-routing-policy/spec.md`（MODIFIED 增量）
- [x] 1.3 本 `tasks.md`

## 2. 代码与配置

- [x] 2.1 删除 `ApiPathPrefixConfiguration.java`
- [x] 2.2 `EchoController` 增加类级 `@RequestMapping("/api/v1")`
- [x] 2.3 更新 `application.properties` 注释
- [x] 2.4 更新 `README.md` 中「技术栈与项目结构 / API 路径」相关表述

## 3. 主规格与校验

- [x] 3.1 合并更新 `openspec/specs/api-routing-policy/spec.md`（与变更增量一致）
- [x] 3.2 `mvn verify` 与 `openspec validate api-prefix-explicit-mapping --strict`
