## 1. Dependencies and config

- [x] 1.1 Add `lock4j-redisson-spring-boot-starter` (2.2.7) to `pom.xml`
- [x] 1.2 Add `lock4j` defaults in `application.yml`: `acquire-timeout: 0`；**join 不写注解 expire**（Redisson watchdog）。yml `expire` 不是 join 的实际 lease
- [x] 1.3 Implement `LockFailureStrategy` → `ApiException(409, ...)`

## 2. Join locking

- [x] 2.1 Annotate `ConvoaiController.join` with `@Lock4j` (name + userId key via `CurrentUserIdAccessor`)
- [x] 2.2 Ensure `GlobalExceptionHandler` maps lock failures to `ApiResponse` 409 if needed

## 3. Docs and tests

- [x] 3.1 Unit-test lock failure strategy (throws ApiException 409)
- [x] 3.2 Update `README.md`: join 防连点语义、409；不写 expire（Redisson 续期）；`LOCK4J_EXPIRE_MS` 管不到 join
- [x] 3.3 Run `mvn spotless:apply` and `mvn verify`

## 4. Scope extension (same release)

- [x] 4.1 Annotate `POST /api/custom-voices` (submit) with `@Lock4j` (`custom-voice-submit` + JWT `sub`)
- [x] 4.2 Document submit anti-duplicate in README；**不**挂启动保存 upsert / corpus 上传 / leave
