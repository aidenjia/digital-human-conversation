# Apply Receipt

**Change**: `vrs-oneshot-error-envelope`
**Iteration**: `1`
**Applied at**: `2026-09-02`
**Executor**: in-session on `feature/vrs-oneshot-error-envelope`

> **已取代（勿作现行契约）**：本收据对应当时的 **420x** 落地。现行已改为 `code` 与 HTTP 对齐 + 友好 `message`（commit `304ad6b`）。本文件仅历史快照。

---

## Workspace

- **Branch**: `feature/vrs-oneshot-error-envelope`
- **Worktree**: none

---

## Tasks

- **Completed**: 10 of 10 in `tasks.md`

---

## Implementation summary

- `ApiException` supports `httpStatus` + optional `error` (body `code` may differ from HTTP)
- `VrsExceptionMapper` maps Tencent Error.Code / Detect reject → ~~4201–4204~~【已废止】→ 现 HTTP/`code` 429/400/502 + 友好 message
- `RealTencentVrsClient` + `CustomVoiceProductionService` pass through mapped exceptions
- Unit tests for mapper + submit no-rewrap; lock4j 409 unchanged
- README custom-voice error table

---

## Verification

- `mvn spotless:apply`
- `mvn verify` (exit 0)
- Focused: `VrsExceptionMapperTest`, `CustomVoiceProductionServiceTest`, `ApiLockFailureStrategyTest`
