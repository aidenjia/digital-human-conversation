# Join Session Warm Implementation Plan

> **For agentic workers:** Implement task-by-task; keep join HTTP contract unchanged.

**Goal:** After Agora ConvoAI join succeeds, synchronously call agent `POST /v1/sessions/warm` (default 5s timeout) before returning RTC credentials; warm failure only WARN.

**Architecture:** `ConvoaiService.join` orchestrates Agora join then `AgentSessionWarmClient.warm(...)`. Client lives in `infrastructure`, uses OkHttp + `convoai.llm.api-key`, resolves URL from `session-warm.url` or derives from `llm.url`. Failures never throw out of join.

**Tech Stack:** Spring Boot 3.5, OkHttp, Jackson, JUnit5/Mockito, OpenSpec superspec.

---

## Task 1: Config & AgentSessionWarmClient

- [ ] **Step 1:** Add nested `SessionWarm` on `ConvoaiProperties` (`enabled=true`, `timeoutMs=5000`, `url` optional) + bind in `application.yml` under `convoai.session-warm`
- [ ] **Step 2:** Create `infrastructure/agent/AgentSessionWarmClient` (or `infrastructure/convoai` sibling package `infrastructure/agent`) with method `warm(clientUserId, conversationId, deviceId, digitalHumanType, timezone)` returning boolean / void; build OkHttp with call timeout from props
- [ ] **Step 3:** Resolve URL: explicit `session-warm.url` else replace trailing `/v1/chat/completions` on `llm.url`; if unresolved or blank api-key → log WARN and no-op
- [ ] **Step 4:** POST JSON body; treat HTTP 2xx and JSON `code==0` as success; otherwise WARN (truncate message); never log Bearer/api-key
- [ ] **Step 5:** Unit-test client URL derivation + success/failure parsing with MockWebServer or mocked OkHttp if project already has a pattern; else cover via Service mock

## Task 2: Wire ConvoaiService

- [ ] **Step 1:** Inject `AgentSessionWarmClient` into `ConvoaiService`
- [ ] **Step 2:** After `restClient.join` success and MDC enrich, if `sessionWarm.enabled` call `warm(...)` inside try/catch; on any exception WARN and continue
- [ ] **Step 3:** Return existing `ConvoaiJoinCredentials` unchanged
- [ ] **Step 4:** Update `ConvoaiServiceTest`: mock warm client — verify called after Agora success with expected args; warm throws → join still returns; `enabled=false` → never called

## Task 3: Docs & verify

- [ ] **Step 1:** README + `docs/convoai-client-integration.md`: note server-side sync best-effort warm after Agora join; no client action; no response field
- [ ] **Step 2:** Add logging row to application-logging mental checklist (INFO/WARN warm events)
- [ ] **Step 3:** `mvn spotless:apply` then `mvn verify`
