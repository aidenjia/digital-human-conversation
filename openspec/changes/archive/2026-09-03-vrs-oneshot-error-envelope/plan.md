# VRS oneshot error envelope Implementation Plan

> **已取代（勿作现行契约）**：本 plan 目标仍是 **4201～4204**。现行已改为 `code` 与 HTTP 对齐 + 友好 `message`（commit `304ad6b`）。本文件仅历史快照。

> **For agentic workers:** Implement task-by-task from `tasks.md`. Do not expand scope to Agora/TTS/COS/i18n.

**Goal:** ~~Map Tencent one-shot VRS failures to dedicated business codes 4201–4204~~【已废止】→ 现改为 HTTP/`code` 对齐（429/400/502）+ English-friendly `ApiResponse` messages; keep lock4j 409 unchanged.

**Architecture:** Extend `ApiException` so `code` (body) and `httpStatus` can diverge; central VRS mapper in `infrastructure.vrs`; `CustomVoiceProductionService` must not re-wrap mapped exceptions.

**Tech Stack:** Java 21, Spring Boot 3.5, Tencent VRS SDK (`TencentCloudSDKException`), JUnit 5 + Mockito

---

### Task 1: ApiException and handler

**Files:**
- Modify: `exception/ApiException.java`
- Modify: `exception/GlobalExceptionHandler.java`

- [ ] **Step 1:** Add `httpStatus` + optional `error`; keep `(code, message)` ctor as `httpStatus=code`
- [ ] **Step 2:** Handler: `ResponseEntity.status(ex.getHttpStatus())` + `fail`/`failWithError`

### Task 2: VRS mapping

**Files:**
- Create: `customvoice/support/CustomVoiceErrorCodes.java`
- Create: `infrastructure/vrs/VrsExceptionMapper.java` (or equivalent name)
- Modify: `infrastructure/vrs/RealTencentVrsClient.java`

- [ ] **Step 1:** Constants 4201–4204 + English messages
- [ ] **Step 2:** Map Error.Code prefixes/exact matches per design.md
- [ ] **Step 3:** Detect rejection → 4202 + reason `VRS_DETECT_REJECTED`

### Task 3: Service pass-through

**Files:**
- Modify: `customvoice/CustomVoiceProductionService.java`

- [ ] **Step 1:** Catch `ApiException` separately and rethrow after `markFailed` (preserve code)

### Task 4: Tests and docs

**Files:**
- Create/Modify: mapper unit test; adjust `CustomVoiceProductionServiceTest` if needed
- Modify: `README.md`
- Existing: `ApiLockFailureStrategyTest` (409)

- [ ] **Step 1:** Mapping + no-rewrap tests
- [ ] **Step 2:** README error table
- [ ] **Step 3:** `mvn spotless:apply` && `mvn verify`
