## 1. Java 源码与测试（包与类名）

- [x] 1.1 将目录 `src/main/java/com/jupiter/digital/human/conversation/` 迁移为 `src/main/java/aidenjia/digitalhuman/conversation/`，将目录 `src/test/java/com/jupiter/digital/human/conversation/` 迁移为 `src/test/java/aidenjia/digitalhuman/conversation/`
- [x] 1.2 批量更新所有受影响 `.java` 的 `package` 与 `import`：`com.jupiter.digital.human.conversation` → `con.aidenjia.digitalhuman.conversation`
- [x] 1.3 重命名入口类：`DigitalHumanApplication.java` → `DigitalHumanConversationApplication.java`，类名与其中 `SpringApplication.run(...)` 参数一致
- [x] 1.4 重命名测试类：`DigitalHumanApplicationTests.java` → `DigitalHumanConversationApplicationTests.java`，并保证 `@SpringBootTest` 仍能发现 `@SpringBootConfiguration`

## 2. Spring 与日志配置

- [x] 2.1 更新 `application.properties` 与 `application-prod.properties`：`spring.application.name=digital-human-conversation`
- [x] 2.2 更新 `logging.level.com.jupiter.digital.human` 为 `logging.level.con.aidenjia.digitalhuman`（或与根包一致的包前缀）
- [x] 2.3 更新 `logback-spring.xml` 中业务 logger 的 `name` 与注释中的包前缀（`con.aidenjia.digitalhuman`）

## 3. 测试断言与文档

- [x] 3.1 更新 `WebApiFoundationIT`（及其他测试）中对 `GET /` 返回 `name` 的断言：由 `digital-human` 改为 `digital-human-conversation`
- [x] 3.2 更新 `README.md` 中根包、示例 `docker` 镜像/容器名等仍含旧品牌或旧包路径的表述（与实现一致即可）

## 4. 容器与 OpenSpec

- [x] 4.1 更新根目录 `Dockerfile`：`ARG JAR_FILE=target/digital-human-conversation-0.0.1-SNAPSHOT.jar`（与 `pom.xml` 的 `artifactId`+`version` 一致）
- [x] 4.2 合并/发布前将 **`openspec/specs/api-routing-policy/spec.md`** 中业务 API 包名改为 `con.aidenjia.digitalhuman.conversation.api`（与本变更 delta 一致）；若流程要求仅在归档时合入，则实现阶段至少保证 delta 与代码已对齐
- [x] 4.3 搜索并更新活动变更目录下对旧包名/旧应用名的引用（例如 `openspec/changes/logging-stdout-only/**/*.md` 中的示例包名）；**不**强制修改 `openspec/changes/archive/**`

## 5. 验证

- [x] 5.1 在仓库根执行全文搜索（如 `digital.human`、`digital-human`、`DigitalHumanApplication`），确认仅剩允许出现的上下文（如 `openspec/changes/archive/**`）
- [x] 5.2 执行 `mvn verify` 通过
