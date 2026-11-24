# ConvoAI MDC Filter Implementation Plan

> **For agentic workers:** Use superpowers:subagent-driven-development to implement this plan task-by-task.

**Goal:** 为 `/api/convoai/**` 建立请求级 SLF4J MDC，使 Logstash 结构化日志可通过 `mdc.deviceId` 等字段检索 join/leave 全链路。

**Architecture:** `ConvoaiMdcFilter`（鉴权后、仅 ConvoAI 前缀）从缓存 body 解析 `deviceId`/`agentId` 并 bind；`ConvoaiService` 在生成 `channel`/`userId` 后 enrich；`finally` 统一 `MDC.clear()`。MDC 写入集中在 `ConvoaiLogContext`。

**Tech Stack:** Java 17、Spring Boot 3.5、SLF4J MDC、`CachedBodyHttpServletRequest`（可重复读 body）、Jackson `ObjectMapper`、JUnit 5、MockMvc。

---

## Task 1: ConvoaiLogContext

- [x] **Step 1:** 在 `trtc/support/ConvoaiLogContext.java` 定义 MDC 键常量：`deviceId`、`digitalHumanType`、`userId`、`channel`、`agentId`
- [x] **Step 2:** 实现 `putIfText`、`clear`、`bindJoinPreview(deviceId, digitalHumanType)`、`bindLeave(agentId)`、`enrichAfterJoin(channel, userId, agentId)`
- [x] **Step 3:** 编写 `ConvoaiLogContextTest`：bind 后 `MDC.get` 有值，`clear` 后为空
- [x] **Step 4:** 运行 `mvn -q test -Dtest=ConvoaiLogContextTest`

## Task 2: ConvoaiMdcFilter

- [x] **Step 1:** 创建 `security/ConvoaiMdcFilter extends OncePerRequestFilter`，`shouldNotFilter` 仅非 `CONVOAI_PREFIX` 跳过
- [x] **Step 2:** `OPTIONS` 直接 `filterChain.doFilter` 不写 MDC
- [x] **Step 3:** 包装 `CachedBodyHttpServletRequest`，`doFilter` 后按 method+path 解析：POST → `deviceId`/`digitalHumanType`；DELETE → `agentId`；`finally` `ConvoaiLogContext.clear()`
- [x] **Step 4:** `WebSecurityConfig` 注册 bean，order `+12`
- [x] **Step 5:** 编写 `ConvoaiMdcFilterTest`：join/leave MDC 绑定与 `finally` 清理；非 ConvoAI 路径不触发
- [x] **Step 6:** 运行相关测试

## Task 3: ConvoaiService enrich 与日志

- [x] **Step 1:** `join` 生成 `channel`/`userId` 后调用 `enrichAfterJoin`；上游返回后 `putIfText(agentId)`
- [x] **Step 2:** 将 `log.info("ConvoAI join requested deviceId=...")` 简化为 `log.info("ConvoAI join requested")`（MDC 承载字段）
- [x] **Step 3:** 更新 `ConvoaiServiceTest`：`@AfterEach` 清理 MDC
- [x] **Step 4:** 运行 `mvn -q test -Dtest=ConvoaiServiceTest`

## Task 4: 文档与全量验证

- [x] **Step 1:** 更新 `application-logging.mdc` MDC 小节为已实现
- [x] **Step 2:** `mvn verify`
