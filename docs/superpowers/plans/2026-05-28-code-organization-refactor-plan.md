# 项目代码整理 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不改变对外 API 契约的前提下，提升可读性、整洁性与扩展性，消除 TRTC 双路径重复逻辑，理顺分层边界，并修复测试漂移。

**Architecture:** 保持单模块 Spring Boot 结构；通过 `trtc.support` 抽取共享能力、`api.trtc.dto` 统一入参/出参模型、`infrastructure` 承接 Redis/SDK 等外部依赖；两个业务 Service 只保留各自差异化的腾讯云请求组装与编排。

**Tech Stack:** Java 17, Spring Boot 3.5.7, Tencent TRTC SDK, Redisson 3.50, JUnit 5, MockMvc

---

## 现状诊断（摘要）

| 问题 | 影响 |
|------|------|
| `TrtcAiConversationService` 与 `TrtcCloudTranscriptionService` 重复 UserSig/校验/SDK 异常处理 | 改一处易漏另一处 |
| 四份同构 record（两个 Result + 两个 Data） | 命名与映射噪音 |
| `GeoLocation` 在 `trtc` 包但被 API 层校验引用 | 层边界渗漏 |
| `StartConversationCommand` 内嵌 vs `StartTranscriptionCommand` 独立文件 | 风格不一致 |
| 无 `EchoController` 但 `WebApiFoundationIT` 仍测 `/api/v1/echo` | CI 失败 |
| Redis 写入失败仅 warn，配置无 TTL/key 外部化 | 运维可观测性弱 |

## 目标包结构（终态）

```
con.aidenjia.digitalhuman.conversation
├── config/                 # Properties, RedissonConfig, TrtcWebConfig
├── security/               # TrtcAuthFilter
├── exception/              # GlobalExceptionHandler, ApiException
├── web/                    # RootController, ActiveProfilesProbeController
├── api/
│   ├── common/             # ApiResponse, ApiResponseWriter, ValidationErrorData
│   └── trtc/
│       ├── TrtcAiConversationController
│       ├── TrtcCloudTranscriptionController
│       └── dto/            # GeoLocation, Start*Request/Data, TrtcJoinCredentials
├── trtc/
│   ├── TrtcAiConversationService
│   ├── TrtcCloudTranscriptionService
│   ├── command/            # StartConversationCommand, StartTranscriptionCommand
│   ├── domain/             # TranscriptionContext（可选）
│   ├── support/            # TrtcUserSigService, TrtcConfigurationValidator
│   └── util/               # TrtcRoomIdGenerator, TrtcTranscriptionUserIdGenerator
└── infrastructure/
    ├── trtc/               # TrtcCloudClientFactory, TrtcJsonConfigLoader
    └── redis/              # TranscriptionContextStore
```

## YAGNI 边界（本计划不做）

- 不引入 Spring Security 全家桶替换 Filter 鉴权
- 不做 Maven 多模块拆分
- 不为 `TranscriptionContextStore` 抽象 Repository 接口
- 不统一 AI 对话为 POST（保持 GET 契约）
- 不全面迁移到 RFC 9457 Problem Details
- 不加转录上下文 read API（尚无消费者）

---

## Phase 0：修复基线（阻塞 CI）

### Task 0: 对齐 Echo 端点与测试

**Files:**
- Create: `src/main/java/aidenjia/digitalhuman/conversation/api/EchoController.java`（若仍需要 echo 能力）
- Modify: `src/test/java/aidenjia/digitalhuman/conversation/WebApiFoundationIT.java`（若 echo 已废弃则删相关用例）
- Test: `WebApiFoundationIT`

- [ ] **Step 1:** 确认 openspec/README 对 echo 的定位（探活示例 vs 已废弃）
- [ ] **Step 2:** 恢复最小 `EchoController`（`POST /api/v1/echo`，`@Valid` name 字段，返回 `ApiResponse`）**或**删除 IT 中对应用例
- [ ] **Step 3:** 运行 `mvn verify`，确认全绿

---

## Phase 1：抽取 TRTC 共享支撑（高 ROI）

### Task 1: TrtcConfigurationValidator

**Files:**
- Create: `src/main/java/aidenjia/digitalhuman/conversation/trtc/support/TrtcConfigurationValidator.java`
- Modify: `TrtcAiConversationService.java`
- Modify: `TrtcCloudTranscriptionService.java`
- Test: Create `TrtcConfigurationValidatorTest.java`

- [ ] **Step 1:** 写失败单测——`validateCommon()` 在缺 SdkAppId 时抛 `TrtcConfigurationException`
- [ ] **Step 2:** 实现 `validateCommon()`（SdkAppId、Cloud 密钥、UserSig）
- [ ] **Step 3:** 实现 `validateForAiConversation()`（+ robot userId）
- [ ] **Step 4:** 实现 `validateForTranscription()`（+ ASR lang/vad）
- [ ] **Step 5:** 两 Service 删除私有 validate 方法，改为注入调用
- [ ] **Step 6:** 运行 `mvn test`，确认通过

### Task 2: TrtcUserSigService

**Files:**
- Create: `src/main/java/aidenjia/digitalhuman/conversation/trtc/support/TrtcUserSigService.java`
- Modify: `TrtcAiConversationService.java`, `TrtcCloudTranscriptionService.java`
- Test: Create `TrtcUserSigServiceTest.java`（Mock `TrtcProperties` 或使用测试密钥）

- [ ] **Step 1:** 写单测——`generateUserSig(userId)` 返回非空字符串
- [ ] **Step 2:** 封装 `TLSSigAPIv2` 创建与 `genUserSig(userId, expireSeconds)`
- [ ] **Step 3:** 两 Service 替换内联 UserSig 逻辑
- [ ] **Step 4:** 运行相关测试

### Task 3: Sdk 调用模板（可选，Task 1–2 完成后评估）

**Files:**
- Create: `src/main/java/aidenjia/digitalhuman/conversation/trtc/support/TrtcSdkExecutor.java`

- [ ] **Step 1:** 抽取 `execute(Supplier<T>)` —— catch `TencentCloudSDKException` → `TrtcUpstreamException`
- [ ] **Step 2:** 两 Service 的 try/catch 块改为一行调用
- [ ] **Step 3:** 确认日志前缀仍区分 `StartAIConversation` / `CreateCloudTranscription`

---

## Phase 2：统一模型与分层（可读性 + 扩展性）

### Task 4: TrtcJoinCredentials 统一进房凭据

**Files:**
- Create: `src/main/java/aidenjia/digitalhuman/conversation/trtc/TrtcJoinCredentials.java`
- Create: `src/main/java/aidenjia/digitalhuman/conversation/api/trtc/dto/TrtcJoinCredentialsResponse.java`（或复用同一 record + 放 api 包）
- Delete: `StartConversationData.java`, `StartTranscriptionData.java`（合并后）
- Modify: 两个 Controller、两个 Service 的 Result record
- Test: 更新 `TrtcAiConversationIT`, `TrtcCloudTranscriptionIT`

- [ ] **Step 1:** 定义单一 record `(sdkAppId, userId, userSig, strRoomId, taskId)`
- [ ] **Step 2:** Service 返回 `TrtcJoinCredentials`；Controller 直接 `ApiResponse.ok(credentials)` 或 `credentials.toResponse()`
- [ ] **Step 3:** 删除重复 DTO/Result 类型
- [ ] **Step 4:** 更新 IT 中 jsonPath 断言（字段名不变则无需改）

### Task 5: 命令对象与 DTO 归位

**Files:**
- Move: `GeoLocation.java` → `api/trtc/dto/GeoLocation.java`
- Move: `StartTranscriptionRequest.java` → `api/trtc/dto/StartTranscriptionRequest.java`
- Create/Move: `trtc/command/StartConversationCommand.java`（从 Service 内嵌抽出）
- Move: `StartTranscriptionCommand.java` → `trtc/command/`
- Modify: import 引用处

- [ ] **Step 1:** 创建目标目录并移动文件（IDE/refactor 保证 import 更新）
- [ ] **Step 2:** `StartConversationCommand` 与 `StartTranscriptionCommand` 对称放置
- [ ] **Step 3:** 编译 + 全量测试

### Task 6: api.common 分包

**Files:**
- Move: `ApiResponse.java`, `ApiResponseWriter.java`, `ValidationErrorData.java` → `api/common/`
- Modify: 全项目 import

- [ ] **Step 1:** 移动三个 common 类
- [ ] **Step 2:** 批量修正 import（`exception`, `security`, `config` 等）
- [ ] **Step 3:** `mvn compile`

---

## Phase 3：基础设施与 Redis 边界

### Task 7: TranscriptionContextStore 迁 infrastructure

**Files:**
- Move: `TranscriptionContextStore.java` → `infrastructure/redis/TranscriptionContextStore.java`
- Move: `TranscriptionContext.java` → `trtc/domain/TranscriptionContext.java`（可选）
- Modify: `TrtcCloudTranscriptionService.java`, `TranscriptionContextStoreTest.java`

- [ ] **Step 1:** 移动 Store 与 domain 类
- [ ] **Step 2:** 更新 import 与测试包路径
- [ ] **Step 3:** 运行 `TranscriptionContextStoreTest`

### Task 8: Redis 配置外部化（小步）

**Files:**
- Modify: `RedisProperties.java` — 增加 `transcriptionContextTtlSeconds`（默认 86400）、`transcriptionContextKeyPrefix`（默认 `transcriptions:`）
- Modify: `TranscriptionContextStore.java` — 从 Properties 读取
- Modify: `application.properties`

- [ ] **Step 1:** 添加配置项与默认值
- [ ] **Step 2:** Store 构造函数注入 Properties
- [ ] **Step 3:** 更新单测断言 TTL/prefix

### Task 9: TrtcCloudClientFactory / TrtcJsonConfigLoader 迁 infrastructure

**Files:**
- Move: `TrtcCloudClientFactory.java`, `TrtcJsonConfigLoader.java` → `infrastructure/trtc/`

- [ ] **Step 1:** 移动并修正 import
- [ ] **Step 2:** 格式化 `TrtcCloudClientFactory`（去除多余空行）
- [ ] **Step 3:** 全量测试

---

## Phase 4：Controller 与测试整洁化

### Task 10: Controller 薄化

**Files:**
- Modify: `TrtcAiConversationController.java`, `TrtcCloudTranscriptionController.java`

- [ ] **Step 1:** 用 `@RequestAttribute(TrtcAuthConstants.CLIENT_USER_ID_ATTRIBUTE) String clientUserId` 替代 `HttpServletRequest`（若 Filter 已保证 attribute 存在）
- [ ] **Step 2:** 两 Controller 结构对齐：构造 command → 调 service → 返回 `ApiResponse.ok`
- [ ] **Step 3:** `@WebMvcTest` 切片测试各 Controller（Mock Service），减少全上下文启动

### Task 11: 测试 support 类

**Files:**
- Create: `src/test/java/aidenjia/digitalhuman/conversation/support/TrtcApiTestSupport.java`

- [ ] **Step 1:** 抽取 `VALID_TOKEN`、`authHeader()`、`postStartTranscription(body)` 等 helper
- [ ] **Step 2:** `TrtcCloudTranscriptionIT`、`TrtcAiConversationIT` 复用
- [ ] **Step 3:** 确认 IT 可读性提升且仍全绿

### Task 12: Service 单测补全

**Files:**
- Create: `TrtcCloudTranscriptionServiceTest.java`（Mock TRTC client + Store）
- Create: `TrtcAiConversationServiceTest.java`

- [ ] **Step 1:** 测 TRTC 成功后 Store.save 被调用
- [ ] **Step 2:** 测 Redis save 抛异常时 Service 仍返回结果（部分成功策略）
- [ ] **Step 3:** 测配置缺失时抛 `TrtcConfigurationException`

---

## Phase 5：文档与收尾

### Task 13: 更新 README / API 说明

**Files:**
- Modify: `README.md`（若存在）或 openspec 相关文档

- [ ] **Step 1:** 文档注明 cloud-transcriptions 为 POST + body 字段
- [ ] **Step 2:** 补充 Redis 环境变量说明
- [ ] **Step 3:** 包结构图与本文 Phase 终态一致

### Task 14: 最终验证

- [ ] **Step 1:** `mvn verify` 全绿
- [ ] **Step 2:** 人工走读两个 Service，确认各 < 100 行核心逻辑
- [ ] **Step 3:** 确认无新增 YAGNI 抽象

---

## 建议执行顺序与工作量

| Phase | 预估 | 优先级 |
|-------|------|--------|
| Phase 0 | 0.5h | P0 — 先绿 CI |
| Phase 1 | 2–3h | P0 — 最大重复消除 |
| Phase 2 | 2h | P1 — 模型与分包 |
| Phase 3 | 1h | P2 — Redis 边界 |
| Phase 4 | 2h | P2 — 测试质量 |
| Phase 5 | 0.5h | P3 — 文档 |

**推荐分批 PR：**

1. PR1: Phase 0 + Phase 1（行为不变，重复减少）
2. PR2: Phase 2（包移动 + JoinCredentials，import 变动大）
3. PR3: Phase 3–4（infrastructure + 测试）

---

## 验收标准

- [ ] 对外 HTTP 契约不变（路径、字段、鉴权方式）
- [ ] `mvn verify` 通过
- [ ] 两 TRTC Service 不再各自维护 UserSig/校验/SDK 异常模板
- [ ] API DTO 与 domain 类不跨层引用混乱
- [ ] 新能力（如 AI 对话也写 Redis）可在 **单一扩展点** 添加，而非复制粘贴
