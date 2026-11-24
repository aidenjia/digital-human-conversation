# Qwen Audio Realtime Session — Implementation Plan

> **For agentic workers:** Use superpowers:subagent-driven-development to implement this plan task-by-task.

**Goal:** Add `POST /api/rtc/credentials` and `POST /api/bailian/realtime-tokens` so clients can use Shengwang 3A + Bailian `qwen-audio-3.0-realtime-flash` WebSocket without ConvoAI agent join.

**Architecture:** Thin Controllers under `api.rtc` / `api.bailian`; orchestration services resolve userId + preferences; `AgoraRtcTokenService` reused for RTC; new `infrastructure.bailian` OkHttp client calls DashScope tokens API. No server-side WebSocket relay.

**Tech Stack:** Java 21, Spring Boot 3.5, existing OkHttp pool, MockMvc + `@WebMvcTest` / service unit tests, Spotless.

**Specs:** `openspec/changes/qwen-audio-realtime-session/specs/rtc-credentials/spec.md`, `bailian-realtime-token/spec.md`  
**Design:** `openspec/changes/qwen-audio-realtime-session/design.md`

---

## File map

| Path | Responsibility |
|------|----------------|
| `config/BailianRealtimeProperties.java` | api-key, workspace-id, model, voice, instructions, ttl |
| `application.yml` | `bailian.realtime.*` placeholders |
| `api/rtc/RtcCredentialsController.java` | HTTP adapter |
| `api/rtc/dto/RtcCredentialsResponse.java` | appId, channel, token, userId |
| `rtc/RtcCredentialsService.java` | channel + user token |
| `api/bailian/BailianRealtimeTokenController.java` | HTTP adapter |
| `api/bailian/dto/*` | request (optional digitalHumanType) + response |
| `bailian/BailianRealtimeTokenService.java` | TTL clamp, hints, call client |
| `infrastructure/bailian/BailianTokenClient.java` | OkHttp POST tokens |
| Tests under `src/test/java/.../api/rtc|bailian/` and service tests | Auth + shape + failures |
| `README.md` | Client contract |

---

## Task 1: BailianRealtimeProperties

- [ ] **Step 1:** Add failing test or compile-time binding smoke that `bailian.realtime.token-ttl-seconds` defaults to 1800 and model default is `qwen-audio-3.0-realtime-flash`
- [ ] **Step 2:** Implement `BailianRealtimeProperties` + `@EnableConfigurationProperties` where other Properties are enabled; add yml keys with `${DASHSCOPE_API_KEY:}` / `${BAILIAN_WORKSPACE_ID:}`
- [ ] **Step 3:** `mvn -q -Dtest=... test` (or next task’s tests); `mvn spotless:apply`
- [ ] **Step 4:** Commit: `Add Bailian realtime properties for temp token issuance`

## Task 2: RTC credentials (TDD)

- [ ] **Step 1:** Write `@WebMvcTest(RtcCredentialsController.class)` cases: no bearer → 401; invalid → 401; valid → 200 + `data.appId/channel/userId` and no `agentId` (mock service)
- [ ] **Step 2:** Implement Controller + Service using `CurrentUserIdAccessor`, `RoomIdGenerator`, `AgoraRtcTokenService`, `ConvoaiProperties.getAppId()` (or shared app-id accessor already used by join)
- [ ] **Step 3:** Confirm security config already covers `/api/**` (no whitelist hole)
- [ ] **Step 4:** Run targeted tests; spotless; commit: `Add POST /api/rtc/credentials without ConvoAI agent`

## Task 3: Bailian token client + service (TDD)

- [ ] **Step 1:** Unit-test `BailianTokenClient` with MockWebServer/OkHttp mock: success JSON `{token, expires_at}`; non-2xx → UpstreamException; missing api-key → ConfigurationException
- [ ] **Step 2:** Implement client against `https://dashscope.aliyuncs.com/api/v1/tokens?expire_in_seconds=`
- [ ] **Step 3:** Unit-test service: TTL clamp to \[1,1800\]; wsUrl format with workspace + model; instructions from companion/dimension mocks; voice from properties; greetingText optional; never logs token
- [ ] **Step 4:** Implement `BailianRealtimeTokenService`
- [ ] **Step 5:** spotless; commit: `Add Bailian temporary token client and session hints`

## Task 4: Bailian HTTP API (TDD)

- [ ] **Step 1:** `@WebMvcTest` auth + success envelope + config/upstream error mapping
- [ ] **Step 2:** Implement Controller + DTOs; optional `digitalHumanType` on body
- [ ] **Step 3:** Integration-style test asserting ConvoAI `AgoraRestClient.join` is never called (mock verify zero interactions)
- [ ] **Step 4:** spotless; commit: `Expose POST /api/bailian/realtime-tokens`

## Task 5: Docs + full verify

- [ ] **Step 1:** Update README: endpoints, env vars, client sequence (RTC → 3A mute publish → temp key → WS session.update → append PCM → play via RTC), heartbeat/reconnect notes, Non-goals
- [ ] **Step 2:** `mvn spotless:apply && mvn verify`
- [ ] **Step 3:** Commit: `Document Bailian realtime dual-endpoint client flow`

---

## Apply notes

- Do **not** add Spring WebSocket server or sticky session CD changes in this change.
- Do **not** modify `ConvoaiService.join` behavior.
- Temporary and permanent keys: never INFO/WARN/ERROR message body; assert in tests if a logging helper exists.
