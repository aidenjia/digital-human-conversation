# Join anti-duplicate (lock4j) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prevent concurrent/duplicate join for the same JWT `sub` using lock4j + existing Redisson. Join omits annotation `expire` so Redisson watchdog covers worst-case join.

**Architecture:** Declarative `@Lock4j` on `ConvoaiController.join`; custom `LockFailureStrategy` → `ApiException(409)`; config under `lock4j.*`.

**Tech Stack:** Spring Boot 3.5, lock4j 2.2.7, Redisson 3.36 (existing)

---

### Task 1: Dependency and config

**Files:**
- Modify: `pom.xml`
- Modify: `src/main/resources/application.yml`

- [ ] **Step 1:** Add lock4j-redisson-spring-boot-starter 2.2.7
- [ ] **Step 2:** Configure `lock4j.acquire-timeout: 0`；join **不写**注解 `expire`（Redisson watchdog）。yml `expire` 不是 join 的实际 lease

### Task 2: Failure strategy and handler

**Files:**
- Create: `config/lock/ApiLockFailureStrategy.java` (or `api`/`config` package)
- Modify: `GlobalExceptionHandler.java` if needed

- [ ] **Step 1:** Implement `LockFailureStrategy` throwing `ApiException(409, "Request already in progress, please retry later")`
- [ ] **Step 2:** Add handler for `LockFailureException` → same envelope if strategy does not always wrap

### Task 3: Annotate join

**Files:**
- Modify: `ConvoaiController.java`

- [ ] **Step 1:** Add `@Lock4j(name = "convoai-join", keys = {"@currentUserIdAccessor.requireUserId()"})` **without** annotation `expire` (Redisson watchdog)

### Task 4: Tests and README

**Files:**
- Create: test for `ApiLockFailureStrategy`
- Modify: `README.md`

- [ ] **Step 1:** Assert strategy throws ApiException code 409
- [ ] **Step 2:** Document anti-duplicate (not response-replay idempotency); join omits `expire` (Redisson watchdog); `LOCK4J_EXPIRE_MS` does not control join
- [ ] **Step 3:** `mvn spotless:apply` && `mvn verify`
