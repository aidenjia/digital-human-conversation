> **已取代（勿作现行契约）**：本 tasks 仍按 **4201～4204** 勾选。现行已改为 `code` 与 HTTP 对齐 + 友好 `message`（commit `304ad6b`）。本文件仅历史快照。

## 1. ApiException and handler

- [x] 1.1 Extend `ApiException` with `httpStatus` (default equals `code`) and optional `error`
- [x] 1.2 Update `GlobalExceptionHandler` to use `getHttpStatus()` and `failWithError` when `error` present; log by httpStatus >= 500

## 2. VRS mapping

- [x] 2.1 Add `CustomVoiceErrorCodes`（~~4201–4204~~【已废止】→ 现常量值为 429/400/502 + English message constants）
- [x] 2.2 Add VRS exception mapper (official Error.Code → ApiException with business code + HTTP + optional reason)
- [x] 2.3 Wire `RealTencentVrsClient` SDK failures and Detect `DetectionCode≠0` through mapper

## 3. Service pass-through

- [x] 3.1 `CustomVoiceProductionService`: on detect/create failures, rethrow mapped `ApiException` without collapsing to generic 400/502 messages

## 4. Tests and docs

- [x] 4.1 Unit tests: quota→~~4201/~~429；quality/detect→~~4202/~~400；invalid params→~~4203/~~400；unknown→~~4204/~~502；submit does not re-wrap（【已废止】列已去掉独立业务码）
- [x] 4.2 Confirm lock4j 409 tests still pass
- [x] 4.3 Update `README.md` custom-voice error table (420x + 409)
- [x] 4.4 `mvn spotless:apply` and `mvn verify`
