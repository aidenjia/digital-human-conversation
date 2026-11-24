# ConvoAI leave agent ownership Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prevent cross-user leave (IDOR) by binding `agentId → JWT sub` in Redis after join and verifying ownership before Agora leave.

**Architecture:** `ConvoaiAgentOwnershipStore` (trtc.support) + Redis/NoOp implementations; orchestrated in `ConvoaiService`. Deny with HTTP 404 when ownership cannot be verified.

**Tech Stack:** Spring Boot 3.5, Redisson (existing), JUnit 5 + Mockito

---

### Task 1: Ownership store

**Files:**
- Create: `trtc/support/ConvoaiAgentOwnershipStore.java`
- Create: `trtc/support/NoOpConvoaiAgentOwnershipStore.java`
- Create: `infrastructure/redis/ConvoaiAgentOwnershipRedisStore.java`
- Create: `config/redis/ConvoaiAgentOwnershipConfiguration.java`
- Modify: `ConvoaiProperties.java`, `application.yml`

- [ ] **Step 1:** Interface + NoOp + Redis (`convoai-agent:{agentId}`) + bean wiring
- [ ] **Step 2:** TTL property default 86400 seconds

### Task 2: Service

**Files:**
- Modify: `ConvoaiService.java`

- [ ] **Step 1:** join bind after Agora success (swallow bind errors)
- [ ] **Step 2:** leave ownership check → 404; unbind after successful Agora leave

### Task 3: Tests and docs

**Files:**
- Modify: `ConvoaiServiceTest.java`
- Create: optional Redis store unit test
- Modify: `README.md`

- [ ] **Step 1:** Unit tests for owner / other / unknown / bind-failure-on-join
- [ ] **Step 2:** README leave ownership + 404
- [ ] **Step 3:** `mvn spotless:apply` && `mvn verify`
