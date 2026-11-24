# Voice Corpus Preview Implementation Plan

> **For agentic workers:** Implement task-by-task from `tasks.md`; keep Controller thin.

**Goal:** 为私有 COS 语料签发短 TTL 预签名 GET URL，供前端试音/回放。

**Architecture:** 薄 `api.customvoice` Controller → `CustomVoiceCorpusPreviewService`（归属校验）→ `TencentCosClient.presignGet`。无新表、无 Redis、无粘滞。双入口：`corpusUri` query 与 `businessCode` path。

**Tech Stack:** Java 21、Spring Boot 3.5、腾讯 COS Java SDK、JUnit/Mockito、统一 `ApiResponse` + JWT。

**Specs / Design:** `specs/voice-corpus-preview/spec.md`、`design.md`（含完整接口约定表）。

---

## Task 1: COS presign infrastructure

- [ ] **Step 1:** 在 `TencentCosProperties` 增加 `presignTtlSeconds`（默认 300）；`application.yml` / local 增加 `tencent.cos.presign-ttl-seconds`
- [ ] **Step 2:** 接口方法 `URL presignGet(String objectKey, long ttlSeconds)`（或返回 `String`）
- [ ] **Step 3:** `RealTencentCosClient`：`GeneratePresignedUrlRequest` + GET + `Date` 过期；复用现有 `client()` / `requireConfigured()`
- [ ] **Step 4:** 单测缺配置失败路径
- [ ] **Step 5:** Commit（可选）：`feat(cos): 支持语料预签名 GET`

## Task 2: Preview API

- [ ] **Step 1:** 新增 `CorpusPreviewResponse` record（`url`, `expiresAt` 用 `Instant`）
- [ ] **Step 2:** `CustomVoiceCorpusPreviewService.previewByCorpusUri`：拒绝 http(s)；`belongsToUser`；算 `expiresAt = now+ttl`；调 `presignGet`
- [ ] **Step 3:** `previewByBusinessCode`：Mapper 按 user+code 查；空 uri → 400；再复用签名逻辑
- [ ] **Step 4:** Controller 两 GET；注意字面 `/corpus-url` 映射
- [ ] **Step 5:** 服务单测覆盖规格 Scenario
- [ ] **Step 6:** Commit（可选）：`feat(custom-voice): 语料试音临时读 URL`

## Task 3: Docs & verify

- [ ] **Step 1:** README 定制音色表增加两行试音 API + 简短响应示例
- [ ] **Step 2:** `mvn spotless:apply` && `mvn verify`
- [ ] **Step 3:** Commit（可选）：`docs: 定制音色试音接口约定`
