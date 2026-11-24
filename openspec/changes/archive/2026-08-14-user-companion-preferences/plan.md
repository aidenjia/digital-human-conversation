# User Companion Preferences Implementation Plan

> **For agentic workers:** Use superpowers:subagent-driven-development
> to implement this plan task-by-task.

**Goal:** 在 JWT 分支上交付角色/音色列表、用户配置 upsert，以及 join 按 businessCode 合并偏好注入。

**Architecture:** `api.companion` 薄 Controller；`companion` 编排校验与 upsert；persistence 对齐现表；`ConvoaiService` 仅扩展 join。身份统一从 JWT `sub` 读取。

**Tech Stack:** Java 21、Spring Boot 3.5、Spring Security OAuth2 Resource Server、MyBatis-Plus、JUnit。

---

## Task 1: Persistence and identity

- [x] **Step 1:** 新增 entity/mapper：`Companion`、`CompanionVoice`、`UserCompanionConfig`
- [x] **Step 2:** 实现 `CurrentUserIdAccessor`（`Jwt.getSubject()`）；写失败单测（无认证）
- [x] **Step 3:** 实现 `TencentTtsParamMapper` + 中性锚点单测（1/50/100）
- [ ] **Step 4:** Commit

## Task 2: Catalog APIs

- [x] **Step 1:** `GET /api/companions` + Service 查 enabled 升序
- [x] **Step 2:** `GET /api/companion-voices` + Service 查 enabled 升序
- [x] **Step 3:** `ResourceServerSecurityConfig` 将 `/api/companions/**`、`/api/companion-voices/**`、`/api/user-companion-configs/**` 设为 authenticated
- [x] **Step 4:** `@WebMvcTest`/`MockMvc`：无 JWT 401；有 JWT 200
- [ ] **Step 5:** Commit

## Task 3: Upsert

- [x] **Step 1:** 写失败测试：缺 businessCode、未知角色、下架音色
- [x] **Step 2:** 实现 upsert Service（音色必选 / local 落库空串 / 文案 null + 1/50/100）
- [x] **Step 3:** `POST /api/user-companion-configs` Controller
- [x] **Step 4:** 测试首次 INSERT 与再次 UPDATE
- [ ] **Step 5:** Commit

## Task 4: Join linkage

- [x] **Step 1:** Request/Command 增加可选 `businessCode`
- [x] **Step 2:** 写测试：无 code 行为不变；有配置覆盖 TTS/greeting；未知 code 400
- [x] **Step 3:** `ConvoaiService` 合并用户配置；idle 用 `props.getIdleTimeout()`
- [ ] **Step 4:** Commit

## Task 5: Docs and verify

- [x] **Step 1:** 更新 README 与 `docs/convoai-client-integration.md`
- [ ] **Step 2:** `mvn spotless:apply` && `mvn verify`
- [ ] **Step 3:** Commit
