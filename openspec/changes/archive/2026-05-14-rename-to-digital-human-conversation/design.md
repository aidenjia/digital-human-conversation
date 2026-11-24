## Context

当前代码根包为 `com.jupiter.digital.human.conversation`，`spring.application.name=digital-human`，规范文档 `api-routing-policy` 仍写 `com.jupiter.digital.human.api`。Maven 产物名为 `digital-human-conversation-0.0.1-SNAPSHOT.jar`，与 `Dockerfile` 默认 `JAR_FILE` 不一致。

## Goals / Non-Goals

**Goals:**

- 应用对外名称与仓库/artifact 一致：**digital-human-conversation**。
- Java 包名与之一致且合法：采用 **`con.aidenjia.digitalhuman.conversation`**（`digitalhuman` ↔ **digital-human**）。
- 规范、测试、日志、Docker、README 与代码同源，无残留旧包名字符串（除 `openspec/changes/archive/**` 历史快照可不动）。

**Non-Goals:**

- 不修改 `groupId`（仍为 `com.jupiter`）。
- 不重写 `openspec/changes/archive/**` 下已归档变更正文（除非团队要求全量刷新历史文档）。

## Decisions

1. **包名映射**  
   采用 `con.aidenjia.digitalhuman.conversation` + `.api` / `.web` / `.exception`，理由：可读、长度适中，且避免 `digital.human` 与产品名 **digital-human-conversation** 长期并存。

2. **主类命名**  
   `DigitalHumanConversationApplication`，理由：保留 Digital 前缀习惯且含 Conversation，与 `DigitalHumanApplication` 相比歧义更小。

3. **目录迁移**  
   物理路径 `src/.../com/jupiter/digital/human/conversation/` → `src/.../con/aidenjia/digitalhuman/conversation/`，由 IDE 或批量移动后全量替换 `package` 与 `import`。

4. **规范更新方式**  
   在本变更的 delta spec 中 **MODIFIED** `api-routing-policy` 对应 Requirement 全文；实现合并后再执行 OpenSpec 归档，将 delta 合入 `openspec/specs/`（与既有工作流一致）。

## Risks / Trade-offs

- **[Risk] 漏改 import** → `mvn verify` + 全仓库 `rg digital\\.human` 门禁式检查。  
- **[Risk] 第三方已依赖旧 FQCN** → 在 proposal 中已标 BREAKING；对内无该依赖则风险低。

## Migration Plan

1. 分支上完成移动与替换 → `mvn verify`。  
2. 若有下游复制了启动类全名，发布说明中列出旧 → 新 FQCN。

## Open Questions

- 若产品方强制要求包名必须包含三段 `digital.human.conversation` 仅改应用名：可缩小范围为只改 `spring.application.name` 与测试断言；本设计已按「包名一并改」执行，若有冲突请在实现前调整 proposal。
