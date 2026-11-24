# Custom Voice Production Implementation Plan

> **For agentic workers:** Use superpowers:subagent-driven-development
> to implement this plan task-by-task.

**Goal:** 交付定制音色生产 API（建单 → 文案 → 检测 → 训练 → 查状态），落库腾讯单号与结果，本期不做支付。

**Architecture:** `api.customvoice` 薄 Controller；`customvoice` 编排 + 枚举映射；`infrastructure` 腾讯 VRS **Real** Client（一句话 TaskType=5，无 Mock）；AudioId 可选 Redis；实体扩展对齐 `companion_custom_voice`。选用/join 不改需求，复用现有 companion 能力。

**Tech Stack:** Java 21、Spring Boot 3.5、OAuth2 Resource Server、MyBatis-Plus、Redisson/Redis、JUnit、腾讯云 VRS 一句话版（官方 SDK 或 TC3 签名 HTTP）。

---

## Task 1: Persistence & config

- [ ] **Step 1:** 扩展 `CompanionCustomVoice` 字段与 getter/setter（含 `trainingTextId`、`providerTaskId` 等）
- [ ] **Step 2:** 新增 `CustomVoiceLanguage` / `CustomVoiceDomain`（或等价常量）+ 腾讯映射单测
- [ ] **Step 3:** 新增 VRS Properties + yml 占位；禁止密钥进日志
- [ ] **Step 4:** 实现 AudioId Redis repository（set/get/delete + TTL）
- [ ] **Step 5:** Commit（若用户要求再提交）

## Task 2: Tencent VRS client

- [x] **Step 1:** 钉死一句话 TaskType=5；Detect 仅 Base64；落库数字 VoiceType（见 design / tasks 2.1）
- [x] **Step 2:** Client 接口；**已删除 Mock**
- [ ] **Step 2b:** Real 实现（官方 SDK）
- [x] **Step 3:** 服务层 Mockito 测试
- [ ] **Step 4:** Commit（若用户要求再提交）

## Task 3: Production APIs

- [ ] **Step 1:** 先写失败测试：无 JWT、非法 domain、非本人 id
- [ ] **Step 2:** `POST /training-text`（不建单）+ 测试
- [ ] **Step 3:** `POST /api/custom-voices` 提交（建单+检测+训练）+ `GET /{businessCode}`；Security authenticated
- [ ] **Step 4:** 测试：看样例不插库、提交递增 `custom_00N`、缺 textId 400、检测失败不训练、轮询成功
- [ ] **Step 5:** Commit（若用户要求再提交）

## Task 4: Docs and verify

- [ ] **Step 1:** 更新 `README.md` 与 `docs/convoai-client-integration.md`（或独立定制音色小节）
- [ ] **Step 2:** `mvn spotless:apply` && `mvn verify`
- [ ] **Step 3:** Commit（若用户要求再提交）
