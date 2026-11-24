## Why

仓库目录与 Maven `artifactId` 已为 `digital-human-conversation`，但 Spring 应用名仍为 `digital-human`，Java 根包仍为 `com.jupiter.digital.human.conversation`，与对外产品/仓库命名不一致；`openspec/specs/api-routing-policy` 等文档仍写旧包路径 `com.jupiter.digital.human.api`，与实现（`...conversation.api`）也不一致。统一为 **digital-human-conversation** 可降低联调与文档认知成本，并一次性修正 Docker 中 JAR 文件名与 Maven 产物不一致的问题。

## What Changes

- **Spring 应用标识**：`spring.application.name` 从 `digital-human` 改为 **`digital-human-conversation`**（`application.properties` / `application-prod.properties`）；根路径 `GET /` 返回的 `name` 字段随之变化，**集成测试中断言需同步**。
- **Java 根包重命名**：`com.jupiter.digital.human.conversation` 改为 **`con.aidenjia.digitalhuman.conversation`**（连字符在 Java 包名中非法，采用 **`digitalhuman`** 表示 **digital-human** 段；子包 **`api`**、**`web`**、**`exception`** 保持不变层级）。
- **入口与测试类命名**：`DigitalHumanApplication` 建议改为 **`DigitalHumanConversationApplication`**（及对应 `...Tests`），与产品名一致，避免「DigitalHuman」与「digital-human-conversation」混用。
- **日志配置**：`logging.level` 与 Logback `<logger name="...">` 中的包前缀改为新根包（如 `con.aidenjia.digitalhuman`）。
- **OpenSpec 现行规范**：更新 **`openspec/specs/api-routing-policy/spec.md`** 中业务 API 包名为新路径（归档本变更时与代码一致）。
- **Docker**：`Dockerfile` 中 `ARG JAR_FILE` 默认值改为与 **`mvn package` 产物**一致（当前为 `target/digital-human-conversation-0.0.1-SNAPSHOT.jar`）。
- **README**：根包说明、示例镜像名等凡写 `com.jupiter.digital.human` 或旧应用名处一并更新。
- **其他 OpenSpec 变更目录**：若仍存在引用旧包名/旧应用名的**活动**变更内 `specs/*.md`（例如 `logging-stdout-only` 中的示例包名），实现时一并替换，避免归档后漂移。

## Capabilities

### New Capabilities

（无；属标识与代码结构统一，不新增业务能力。）

### Modified Capabilities

- **`api-routing-policy`**：Requirement 中「业务控制器所在包」的规范字符串 SHALL 与实现一致，并随根包重命名更新为 `con.aidenjia.digitalhuman.conversation.api`（及与「显式 `@RequestMapping("/api/v1")`」表述一致）。

## Impact

- **BREAKING**：外部若按类名/包名反射、脚本扫描路径、或硬编码 `digital-human` / `com.jupiter.digital.human`，需跟随升级。
- 受影响：`src/main/java/**`、`src/test/java/**`、`src/main/resources/*`、`Dockerfile`、`README.md`、`openspec/specs/api-routing-policy/spec.md`；可选扫 `openspec/changes/**` 中非 archive 的引用。
- `pom.xml` 的 `artifactId`/`name` 已为 `digital-human-conversation`，本次以包名与应用配置对齐为主；`groupId` 保持 `com.jupiter`（除非产品另有要求，本变更不强制改 groupId）。
