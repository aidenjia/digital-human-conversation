# Agora And Bailian WebRTC Only Cleanup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Remove the Bailian PCM WebSocket solution, PostgreSQL-backed product/configuration domains, custom voice and every non-conversation business API, while retaining Agora ConvoAI/RTC and Bailian WebRTC end to end.

**Architecture:** The retained HTTP surface is `POST|DELETE /api/convoai/agents`, `POST /api/rtc/credentials`, and `POST /api/bailian/realtime/webrtc`, plus the WebRTC-only control socket `WS /ws/bailian/realtime-events`. Agora join keeps the existing request contract but always builds its TTS/ASR/session profile from `ConvoaiProperties`, so it no longer reads companion or dimension records. Bailian WebRTC keeps its control socket because captions, text turns, greetings, interruption and avatar actions currently depend on it; only the PCM media relay `WS /ws/bailian/realtime` is removed.

**Tech Stack:** Java 21, Spring Boot 3.5, Maven, React 19, TypeScript 6, Vite 8, Agora Web SDK, Bailian Realtime WebRTC, Redis/Redisson where configured.

## Execution Record (2026-10-30)

Implementation Tasks 1-6 and automated verification are complete; review fixes are committed. See [verification record](../../voice-only-cleanup-verification.md). Real authenticated provider-call acceptance remains unperformed (Task 7 Steps 5-6), distinct from the passing local browser navigation smoke.

Execution safety adjustments: stage explicit task-owned paths instead of blanket `git add -A`; retain user changes as unstaged diffs. Use `clean verify` (including Spotless check) rather than blanket formatting. Static scans exclude archived/planning documents, generated dependencies and intentionally negative regression assertions; `com.baomidou` Lock4j remains permitted.

## Global Constraints

- This plan does not change `digital-human-agent` or the external `digital-human-cd` repository.
- Preserve the current Agora join/leave, RTC token, interruption, agent ownership and session-warm behavior.
- Preserve Bailian WebRTC SDP exchange, one-time relay ticket, WebRTC control events, text input, subtitles, greeting isolation, barge-in and avatar action acknowledgement.
- Delete `WS /ws/bailian/realtime`; retain `WS /ws/bailian/realtime-events` because it is part of the WebRTC implementation, not the PCM media relay.
- Retain Redis/Redisson for Agora ownership, the join lock and the Bailian WebRTC one-time ticket. PostgreSQL removal does not imply Redis removal.
- Retain `tencent.credentials.*` and `CONVOAI_TTS_APP_ID`; Agora ConvoAI still sends these credentials to its Tencent TTS vendor configuration.
- Preserve the browser Avatar runtime, checked-in wave animation and current local motion playback work. Delete the server-side motion-generation API/client and its UI panel only.
- Do not delete `conversation-ui/public/models/avatar-sample-2.vrm`; it remains local and ignored.
- Do not edit or delete `openspec/changes/archive/**`; it is historical evidence.
- Do not discard the current dirty worktree. The executor must first preserve the user's uncommitted Avatar/motion work.

---

## Target Surface

| Kind | Retain | Remove |
|---|---|---|
| HTTP | `/api/convoai/agents`, `/api/rtc/credentials`, `/api/bailian/realtime/webrtc` | `/api/echo`, companion, dimension, custom voice, voice preview, scene image and motion job endpoints |
| WebSocket | `/ws/bailian/realtime-events` | `/ws/bailian/realtime` |
| Frontend | login, mode selection, Agora conversation, Bailian WebRTC conversation, Avatar runtime | PCM relay mode, digital-human configuration page, catalog/voice-preview/config hooks, motion-generation panel |
| Storage | optional Redis stores used by retained voice flows | PostgreSQL, MyBatis-Plus, H2 test database, persisted companion/dimension/voice/scene entities |
| External SDKs | Agora authentication, OkHttp, Tencent credential properties for Agora TTS | Tencent VRS SDK, Tencent TTS Java SDK, COS SDK, code generator dependencies |

## File Structure

**Retained and modified**

- `src/main/java/aidenjia/digitalhuman/conversation/trtc/ConvoaiService.java`: stop resolving profiles from companion/dimension persistence.
- `src/main/java/aidenjia/digitalhuman/conversation/trtc/support/JoinSessionProfile.java`: become a persistence-free default profile.
- `src/main/java/aidenjia/digitalhuman/conversation/infrastructure/bailian/BailianRealtimeEventCommandFactory.java`: contain only JSON commands used by WebRTC control events.
- `src/main/java/aidenjia/digitalhuman/conversation/api/bailian/webrtc/BailianRealtimeEventsWebSocketConfig.java`: register only `/ws/bailian/realtime-events`.
- `src/main/java/aidenjia/digitalhuman/conversation/bailian/webrtc/BailianWebRtcDrainLifecycle.java`: drain only WebRTC event sessions.
- `src/main/java/aidenjia/digitalhuman/conversation/bailian/webrtc/BailianWebRtcIdleReaper.java`: reap only WebRTC event sessions.
- `conversation-ui/src/ConversationRealtime.tsx`: become WebRTC-only and be renamed to `BailianWebRtcConversation.tsx`.
- `conversation-ui/src/Conversation.tsx`: remain the Agora page and offer only local session-type selection.
- `conversation-ui/src/services/agoraRTC.ts`: keep normal Agora publish/subscribe; remove PCM relay capture/playback methods.
- `conversation-ui/src/services/bailianRealtimeConfig.ts`: own the model/persona/voice constants currently exported by the PCM WebSocket client.
- `conversation-ui/src/services/captionLines.ts`: own `CaptionLine`; WebRTC must not import a deleted hook for this type.

**Deleted by directory or file**

- Backend business APIs/domains: `api/companion`, `api/customvoice`, `api/dimension`, `api/motion`, `api/scene`, `api/voicepreview`, `companion`, `customvoice`, `dimension`, `motion`, `scene`, `voicepreview`, and `api/EchoController.java`.
- Persistence/integration: `infrastructure/persistence`, `infrastructure/cos`, `infrastructure/motion`, `infrastructure/tts`, `infrastructure/vrs`, companion/dimension Redis stores/configurations, COS/motion/voice-preview configuration classes.
- PCM relay: `bailian/relay`, PCM handler/interceptor classes under `api/bailian/ws`, and their tests.
- Frontend configuration/PCM files: companion/dimension/motion/voice-preview API modules and hooks, `DigitalHumanConfig.*`, `MotionGenerationPanel.*`, `useRealtimeConversation.ts`, `bailianRealtimeWs.ts`, `pcmPlayback.ts`, `pcmBridge.ts`, `pcmPlayer.ts`.
- Database assets: `docs/sql`, `tools/codegen`, `src/test/resources/templates`, database generator test and PostgreSQL-specific authoritative spec.

### Task 1: Preserve The Dirty Worktree And Lock The Retained Contract

**Files:**
- Create: `src/test/java/aidenjia/digitalhuman/conversation/api/RetainedApiSurfaceTest.java`
- Modify: `src/test/java/aidenjia/digitalhuman/conversation/api/bailian/ws/BailianRealtimeEventsHandshakeInterceptorTest.java`
- Test: `src/test/java/aidenjia/digitalhuman/conversation/api/RetainedApiSurfaceTest.java`

**Interfaces:**
- Consumes: the current controller mappings and the existing WebRTC events handshake.
- Produces: an executable allowlist for the three retained business HTTP routes and the WebRTC control socket.

- [x] **Step 1: Record and preserve all pre-existing changes**

Run:

```powershell
git status --short
git diff --stat
git diff -- conversation-ui/src/hooks/useWebRtcConversation.ts conversation-ui/src/hooks/useRealtimeConversation.ts conversation-ui/src/services/bailianRealtimeWs.ts
```

Expected: the current Avatar/motion edits remain visible. Before deletion work starts, either commit them to the current feature branch or create a named patch outside the repository; do not use `git reset`, `git checkout --`, or blanket cleanup.

- [x] **Step 2: Write the HTTP allowlist test**

Create a Spring Boot test that filters `RequestMappingHandlerMapping` to classes below `con.aidenjia.digitalhuman.conversation.api` and expects exactly these business mappings after cleanup:

```java
Set<String> expected =
        Set.of(
                "POST /api/convoai/agents",
                "DELETE /api/convoai/agents",
                "POST /api/rtc/credentials",
                "POST /api/bailian/realtime/webrtc");
assertThat(actualBusinessMappings).isEqualTo(expected);
```

Exclude framework error mappings and the root/actuator endpoints from `actualBusinessMappings`.

- [x] **Step 3: Run the test and confirm it fails before cleanup**

Run:

```powershell
mvn -B -ntp -Dtest=RetainedApiSurfaceTest test
```

Expected: FAIL because companion, dimension, custom voice, voice preview, scene, motion and echo mappings still exist.

- [x] **Step 4: Add a WebRTC-control regression to the existing handshake test**

Keep assertions for authentication, one-time `relayId`, user match, `persona`, `voice`, `enableAction`, ticket model and drain rejection. Remove any helper coupling to `BailianRealtimeHandshakeInterceptor`; the events handshake must own its parsing rules.

- [x] **Step 5: Commit the test gate**

```powershell
git add src/test/java/aidenjia/digitalhuman/conversation/api/RetainedApiSurfaceTest.java src/test/java/aidenjia/digitalhuman/conversation/api/bailian/ws/BailianRealtimeEventsHandshakeInterceptorTest.java
git commit -m "test: lock retained voice api surface"
```

### Task 2: Decouple Agora ConvoAI From Companion And Dimension Storage

**Files:**
- Modify: `src/main/java/aidenjia/digitalhuman/conversation/trtc/ConvoaiService.java`
- Modify: `src/main/java/aidenjia/digitalhuman/conversation/trtc/support/JoinSessionProfile.java`
- Modify: `src/main/java/aidenjia/digitalhuman/conversation/trtc/support/ConvoaiJoinBodyAssembler.java`
- Modify: `src/test/java/aidenjia/digitalhuman/conversation/trtc/ConvoaiServiceTest.java`
- Modify: `src/test/java/aidenjia/digitalhuman/conversation/trtc/support/ConvoaiJoinBodyAssemblerTest.java`

**Interfaces:**
- Consumes: `JoinConvoaiCommand`, `ConvoaiProperties`, `TencentCloudCredentialsProperties`.
- Produces: the unchanged join response and upstream body, built only from configuration defaults.

- [x] **Step 1: Add a failing parameterized service test**

For `UE5`, `Unity` and `MMD`, assert that join succeeds using the same configured profile and that the upstream LLM parameters still contain the caller's `digitalHumanType`:

```java
@ParameterizedTest
@EnumSource(DigitalHumanType.class)
void joinUsesConfiguredVoiceProfileWithoutPersistence(DigitalHumanType type) {
    ConvoaiJoinCredentials result = service.join(new JoinConvoaiCommand("device-1", type, "Asia/Shanghai"));
    assertThat(result.agentId()).isEqualTo("agent-1");
    verify(restClient).join(argThat(body -> llmType(body).equals(type.name())));
}
```

The test fixture must construct `ConvoaiService` without `CompanionJoinPreferenceService` or `DimensionJoinPreferenceService`.

- [x] **Step 2: Run the focused tests and confirm the constructor/profile failure**

```powershell
mvn -B -ntp '-Dtest=ConvoaiServiceTest,ConvoaiJoinBodyAssemblerTest' test
```

Expected: FAIL until the persistence-backed constructor arguments and user profile implementation are removed.

- [x] **Step 3: Simplify the join profile**

Make `JoinSessionProfile` expose one configuration-backed implementation only:

```java
public enum JoinSessionProfile {
    CONFIGURED;

    public String replyLanguage() {
        return "zh-cn";
    }
}
```

Keep the existing greeting, comfort text, TTS voice/volume/speed/pitch and ASR language behavior, but read each value from `ConvoaiProperties`; remove every import from `companion.*` and `dimension.*`.

- [x] **Step 4: Remove persistence resolution from `ConvoaiService`**

Delete both preference-service fields, constructor arguments and `resolveJoinProfile`. Pass `JoinSessionProfile.CONFIGURED` to the assembler for every `DigitalHumanType`. Keep `command.digitalHumanType()` in LLM params and session warm for downstream compatibility.

- [x] **Step 5: Run the focused tests**

```powershell
mvn -B -ntp '-Dtest=ConvoaiServiceTest,ConvoaiJoinBodyAssemblerTest,ConvoaiControllerTest' test
```

Expected: PASS; join/leave response contracts remain unchanged and no test mock references a persistence preference service.

- [x] **Step 6: Commit the decoupling**

```powershell
git add src/main/java/aidenjia/digitalhuman/conversation/trtc src/test/java/aidenjia/digitalhuman/conversation/trtc
git commit -m "refactor: make agora sessions configuration only"
```

### Task 3: Delete PostgreSQL And Non-Conversation Backend Domains

**Files:**
- Delete: `src/main/java/aidenjia/digitalhuman/conversation/infrastructure/persistence/`
- Delete: `src/main/java/aidenjia/digitalhuman/conversation/api/{companion,customvoice,dimension,motion,scene,voicepreview}/`
- Delete: `src/main/java/aidenjia/digitalhuman/conversation/{companion,customvoice,dimension,motion,scene,voicepreview}/`
- Delete: `src/main/java/aidenjia/digitalhuman/conversation/infrastructure/{cos,motion,tts,vrs}/`
- Delete: `src/main/java/aidenjia/digitalhuman/conversation/api/EchoController.java`
- Delete: `src/main/java/aidenjia/digitalhuman/conversation/config/{CosClientConfig,CosProperties,HunyuanMotionProperties,VoicePreviewProperties}.java`
- Delete: `src/main/java/aidenjia/digitalhuman/conversation/config/redis/{UserCompanionConfigCacheConfiguration,UserDimensionConfigCacheConfiguration}.java`
- Delete: `src/main/java/aidenjia/digitalhuman/conversation/infrastructure/redis/{UserCompanionConfigRedisStore,UserDimensionConfigRedisStore}.java`
- Delete: matching tests below `src/test/java/aidenjia/digitalhuman/conversation/`
- Modify: `pom.xml`
- Modify: `src/main/resources/application.yml`
- Modify: `src/main/resources/application-{local,dev,test,prod}.yml`
- Modify: `src/test/resources/application-test.yml`
- Modify: `src/main/java/aidenjia/digitalhuman/conversation/config/ResourceServerSecurityConfig.java`

**Interfaces:**
- Consumes: the persistence-free Agora service from Task 2.
- Produces: a Spring context with no `DataSource`, MyBatis mapper, COS, VRS, standalone TTS or motion-generation beans.

- [x] **Step 1: Remove the business modules and their tests**

Use explicit `git rm` paths for the directories listed above. Also remove API auth tests and service tests for those modules. Do not delete Avatar classes or `conversation-ui/public/animations/`.

- [x] **Step 2: Remove database and unused SDK dependencies**

From `pom.xml`, delete properties and dependencies for PostgreSQL, MyBatis-Plus runtime/generator, H2, FreeMarker, Tencent VRS, Tencent TTS Java SDK and COS. Retain Agora authentication, commons-codec, OkHttp, Jasypt, validation, security, actuator, WebSocket and Redisson/Lock4j.

- [x] **Step 3: Remove PostgreSQL and deleted-domain configuration**

Delete all `spring.datasource`, `mybatis-plus`, `digital-human.cos`, `digital-human.motion` and `digital-human.voice-preview` blocks from base and profile YAML. Keep this credential block because Agora TTS still consumes it:

```yaml
tencent:
  credentials:
    secret-id: ${TENCENT_SECRET_ID:}
    secret-key: ${TENCENT_SECRET_KEY:}
```

In `src/test/resources/application-test.yml`, remove H2 and COS settings; keep security test values and test Tencent credentials needed by ConvoAI assembler tests.

- [x] **Step 4: Remove the echo security exception**

Delete `"/api/echo"` from the public matcher and update comments so the only public HTTP endpoints are root and actuator health/metrics. Keep `/api/**` authenticated by default.

- [x] **Step 5: Delete database/code-generation artifacts**

Delete `docs/sql/`, `tools/codegen/`, `src/test/resources/templates/`, and `src/test/java/aidenjia/digitalhuman/conversation/tools/codegen/CodeGenerator.java`. Remove the codegen-specific ignore line from `.gitignore`; leave `/data/motion/` ignored so local generated files are not destructively removed during this cleanup.

- [x] **Step 6: Run static absence checks**

```powershell
rg -n "org\.postgresql|com\.baomidou|jdbc:postgresql|mybatis-plus|datasource:|CustomVoice|DimensionJoin|CompanionJoin|VoicePreview|UserSceneImage|MotionGeneration" pom.xml src/main src/test
```

Expected: no PostgreSQL/MyBatis or removed-domain code references.

- [x] **Step 7: Run backend tests**

```powershell
mvn -B -ntp test
```

Expected: PASS without PostgreSQL or H2.

- [x] **Step 8: Commit backend domain removal**

```powershell
git add -A pom.xml src/main src/test tools/codegen docs/sql .gitignore
git commit -m "refactor: remove persistence and non-conversation domains"
```

### Task 4: Remove The Bailian PCM WebSocket Relay Without Breaking WebRTC Control

**Files:**
- Create: `src/main/java/aidenjia/digitalhuman/conversation/api/bailian/webrtc/BailianRealtimeEventsWebSocketConfig.java`
- Move/Modify: WebRTC events handler and interceptor from `api/bailian/ws` to `api/bailian/webrtc`
- Create: `src/main/java/aidenjia/digitalhuman/conversation/infrastructure/bailian/BailianRealtimeEventCommandFactory.java`
- Create: `src/main/java/aidenjia/digitalhuman/conversation/bailian/webrtc/BailianWebRtcDrainLifecycle.java`
- Create: `src/main/java/aidenjia/digitalhuman/conversation/bailian/webrtc/BailianWebRtcIdleReaper.java`
- Delete: `src/main/java/aidenjia/digitalhuman/conversation/bailian/relay/`
- Delete: PCM-only handler/interceptor classes under `src/main/java/aidenjia/digitalhuman/conversation/api/bailian/ws/`
- Delete: `src/main/java/aidenjia/digitalhuman/conversation/infrastructure/bailian/BailianRealtimeUpstreamClient.java`
- Modify: `src/main/java/aidenjia/digitalhuman/conversation/bailian/webrtc/BailianRealtimeEventsSession.java`
- Modify: `src/main/java/aidenjia/digitalhuman/conversation/bailian/support/BailianRealtimeWsUrls.java`
- Modify/Delete: matching relay and WebSocket tests

**Interfaces:**
- Consumes: Bailian WebRTC data-channel JSON forwarded through `/ws/bailian/realtime-events`.
- Produces: downstream commands for `session.update`, greeting, cancel, text turns and avatar actions; no upstream WebSocket and no PCM methods.

- [x] **Step 1: Make the WebRTC events interceptor self-contained**

Move the attributes and parser into the WebRTC package:

```java
static final String ATTR_USER_ID = "userId";
static final String ATTR_ENABLE_ACTION = "enableAction";
static final String ATTR_MODEL = "model";
static final String ATTR_PERSONA = "persona";
static final String ATTR_VOICE = "voice";

static boolean parseEnableAction(String raw) {
    return raw != null && ("true".equalsIgnoreCase(raw.trim()) || "1".equals(raw.trim()));
}
```

It must no longer reference the PCM `BailianRealtimeHandshakeInterceptor`.

- [x] **Step 2: Register only the WebRTC control path**

The new config must contain one registration:

```java
registry.addHandler(eventsHandler, "/ws/bailian/realtime-events")
        .addInterceptors(eventsHandshakeInterceptor)
        .setAllowedOriginPatterns("*");
```

No constant or mapping for `/ws/bailian/realtime` remains.

- [x] **Step 3: Extract the WebRTC JSON command factory**

Move the pure methods `sessionUpdateJson`, `deleteItemJson`, `actionResultJson`, `greetingJson`, `actionToolsUpdateJson` and `cancelResponseJson` into `BailianRealtimeEventCommandFactory`. Delete `connect`, `sendSessionUpdate`, `appendAudio`, direct `okhttp3.WebSocket` send helpers and `closeQuietly`.

- [x] **Step 4: Rename lifecycle components around WebRTC**

`BailianWebRtcDrainLifecycle` and `BailianWebRtcIdleReaper` must depend only on `BailianRealtimeEventsRegistry`. Their tests must assert `server.draining`, grace close and idle close for event sessions without constructing a relay registry.

- [x] **Step 5: Remove the PCM relay implementation and tests**

Delete relay session/factory/registry, PCM handler/interceptor, relay-specific lifecycle tests and the upstream-WS URL assertion. Keep the WebRTC signaling URL and fixed session instructions, renaming `BailianRealtimeWsUrls` to `BailianRealtimeUrls` if that removes the last misleading PCM name.

- [x] **Step 6: Run focused Bailian tests**

```powershell
mvn -B -ntp '-Dtest=*Bailian*Test,*Realtime*Test,*Avatar*Test' test
```

Expected: PASS, with no test opening or mocking an upstream Bailian WebSocket.

- [x] **Step 7: Commit the transport cleanup**

```powershell
git add -A src/main/java/aidenjia/digitalhuman/conversation/api/bailian src/main/java/aidenjia/digitalhuman/conversation/bailian src/main/java/aidenjia/digitalhuman/conversation/infrastructure/bailian src/test/java/aidenjia/digitalhuman/conversation/api/bailian src/test/java/aidenjia/digitalhuman/conversation/bailian
git commit -m "refactor: remove bailian pcm websocket relay"
```

### Task 5: Reduce The Frontend To Agora And Bailian WebRTC

**Files:**
- Modify: `conversation-ui/src/App.tsx`
- Modify: `conversation-ui/src/pages/ModeSelect.tsx`
- Modify: `conversation-ui/src/utils/session.ts`
- Rename/Modify: `conversation-ui/src/ConversationRealtime.tsx` to `conversation-ui/src/BailianWebRtcConversation.tsx`
- Modify: `conversation-ui/src/Conversation.tsx`
- Modify: `conversation-ui/src/hooks/useConversation.ts`
- Modify: `conversation-ui/src/hooks/useWebRtcConversation.ts`
- Modify: `conversation-ui/src/services/agoraRTC.ts`
- Create: `conversation-ui/src/services/bailianRealtimeConfig.ts`
- Modify: `conversation-ui/src/services/captionLines.ts`
- Delete: frontend configuration, catalog, voice-preview, motion-generation and PCM files listed in File Structure
- Modify: `conversation-ui/types/api.d.ts`
- Modify: `conversation-ui/types/auth.d.ts`
- Delete: `conversation-ui/tests/pcm-playback.test.ts`

**Interfaces:**
- Consumes: only the retained backend routes and WebRTC control socket.
- Produces: two selectable voice paths: Agora ConvoAI and Bailian WebRTC.

- [x] **Step 1: Extract WebRTC-shared constants and types before deleting PCM code**

Create `bailianRealtimeConfig.ts` with the current allowed model tuple, `RealtimeModel`, default persona and default Bailian voice. Import it from the WebRTC hook/page/events client. Import `CaptionLine` directly from `captionLines.ts`.

- [x] **Step 2: Make the Bailian page WebRTC-only**

Remove the `transport: 'pcm' | 'webrtc'` prop and the unconditional creation of both hooks. The component must call only:

```tsx
const conversation = useWebRtcConversation()
```

Delete PCM labels and branches. Keep voice/text input, subtitles, Avatar runtime, model/persona/voice controls, barge-in and end-session behavior. Remove `MotionGenerationPanel`; do not remove local Avatar motion playback.

- [x] **Step 3: Simplify app navigation**

Use only these modes and stages:

```ts
export type VoiceMode = 'agora' | 'webrtc'
type AppStage = 'login' | 'mode' | 'conversation-agora' | 'conversation-webrtc'
```

`ModeSelect` renders exactly two cards. Delete the PCM card and the configuration stage. An old stored value (`realtime` or `legacy`) must fall back to `mode` instead of routing to a deleted page.

- [x] **Step 4: Keep Agora type selection local**

Move the `UE5`/`Unity`/`MMD` choice into `Conversation.tsx` as a local select shown before joining. It may continue sending `digitalHumanType` in `JoinConvoaiRequest`; it must not fetch or save companion/dimension configuration.

- [x] **Step 5: Strip PCM behavior from `AgoraRTCService`**

Remove PCM capture, resampling, custom PCM playback, `enableMicrophoneForRealtime`, `setMicrophoneEnabled`, `preparePlayback`, `startPcmCapture`, `stopPcmCapture`, `playPcm24k` and `flushPlaybackQueue`. Keep normal RTC join, microphone publish/mute, remote audio subscribe/play and leave.

- [x] **Step 6: Delete dead frontend modules and types**

Delete the companion/dimension/motion/voice-preview API modules and hooks, configuration page, motion-generation panel, PCM WebSocket client and PCM utilities. Prune `types/api.d.ts` to auth, Agora join/leave, RTC credentials and common response types.

- [x] **Step 7: Run frontend verification**

```powershell
Set-Location conversation-ui
node --test tests/*.test.ts
npm run lint
npm run build
```

Expected: all remaining Avatar, caption, realtime text and WebRTC tests pass; lint and production build succeed.

- [x] **Step 8: Commit the frontend reduction**

```powershell
git add -A conversation-ui
git commit -m "refactor: keep only agora and bailian webrtc ui"
```

### Task 6: Remove Stale Documentation And Specifications

**Files:**
- Rewrite: `README.md`
- Rewrite: `docs/frontend-realtime-voice-feishu.md`
- Modify: `docs/k8s-graceful-shutdown-requirements.md`
- Delete: `docs/apifox-companion-config-test.md`
- Delete: `docs/user-companion-config-schema.md`
- Delete: `docs/hy-motion-integration.md`
- Delete: removed-capability specs under `openspec/specs/`
- Create: `openspec/changes/retain-agora-bailian-webrtc-only/{proposal.md,design.md,tasks.md}` and delta specs

**Interfaces:**
- Consumes: the final route/dependency surface from Tasks 2-5.
- Produces: documentation that does not advertise removed endpoints, PostgreSQL or PCM WebSocket media relay.

- [x] **Step 1: Rewrite the README around the two retained paths**

Document only:

```text
Agora: POST /api/convoai/agents -> Agora RTC -> DELETE /api/convoai/agents
Bailian: POST /api/bailian/realtime/webrtc -> WS /ws/bailian/realtime-events -> WebRTC media/data channels
```

Also document `/api/rtc/credentials`, JWT/demo authentication, Redis optionality/cluster behavior, Tencent credentials required by Agora TTS, tests and runtime environment variables. Remove PostgreSQL, COS, VRS, custom voice, companion/dimension, voice preview, scene, motion-generation and PCM relay sections.

- [x] **Step 2: Update operational documentation**

Remove PCM flow diagrams and `/ws/bailian/realtime` from the frontend and graceful-shutdown documents. Keep the WebRTC events socket lifecycle and its drain/reconnect behavior.

- [x] **Step 3: Reconcile OpenSpec**

Delete current authoritative specs for PostgreSQL, companion/dimension catalogs/config/join overrides, custom voice, voice preview and user scene images. Add a removal change that explicitly preserves archived change history and records the retained route allowlist. Do not edit `openspec/changes/archive/**`.

- [x] **Step 4: Flag external deployment cleanup**

Record that the separate `digital-human-cd` repository must remove `SPRING_DATASOURCE_*`, PostgreSQL/pgpool assumptions, `COS_*`, voice-preview, VRS and motion-generation variables from this service deployment. Do not mutate the sibling repository as part of this plan without separate scope.

- [x] **Step 5: Commit documentation**

```powershell
git add -A README.md docs openspec
git commit -m "docs: describe voice-only conversation service"
```

### Task 7: Final Static, Automated And Browser Verification

Local browser navigation passed for old/empty stored modes, two cards and both pages without real cloud calls; the full real-call scenarios below remain unchecked.

**Files:**
- Modify if needed: only files implicated by failed checks
- Test: all remaining backend and frontend tests

**Interfaces:**
- Consumes: the complete reduced application.
- Produces: evidence that removed capabilities are absent and both retained voice paths still work.

- [x] **Step 1: Format and run the complete backend gate**

```powershell
mvn -B -ntp clean verify
```

Expected: `BUILD SUCCESS`; Spring context tests start without PostgreSQL/H2.

- [x] **Step 2: Run the complete frontend gate**

```powershell
Set-Location conversation-ui
node --test tests/*.test.ts
npm run lint
npm run build
Set-Location ..
```

Expected: all commands exit 0.

- [x] **Step 3: Prove removed symbols and routes are absent**

```powershell
rg -n "jdbc:postgresql|org\.postgresql|com\.baomidou\.mybatisplus|mybatis|custom-voices|dimension-voices|user-dimension-configs|/api/companions|companion-voices|user-companion-configs|voice-previews|user-scene-images|motion/jobs|api/echo" pom.xml src/main conversation-ui/src conversation-ui/types README.md docs/frontend-realtime-voice-feishu.md
rg -n --pcre2 "/ws/bailian/realtime(?!-events)" src/main conversation-ui/src README.md docs/frontend-realtime-voice-feishu.md
```

Expected: neither command returns a match.

- [x] **Step 4: Prove required paths still exist**

```powershell
rg -n "/api/convoai/agents|/api/rtc/credentials|/api/bailian/realtime/webrtc|/ws/bailian/realtime-events" src conversation-ui README.md
```

Expected: every retained path appears in implementation, tests and documentation.

- [ ] **Step 5: Browser-smoke Bailian WebRTC**

Verify voice and text input, user/assistant subtitles, remote RTP playback, welcome-message isolation, user interruption, old-response isolation, avatar action start acknowledgement, end call and reconnect. Confirm the browser never calls `/ws/bailian/realtime`, `/api/rtc/credentials`, configuration catalogs or motion-generation APIs on this path.

- [ ] **Step 6: Browser-smoke Agora**

Verify join, microphone publish/mute/unmute, remote AI audio, interruption and leave for at least one `digitalHumanType`. Confirm no companion/dimension/catalog/voice-preview request is made and the join uses configuration defaults.

- [x] **Step 7: Check the final diff and dependency graph**

```powershell
git status --short
git diff --check
mvn -B -ntp dependency:tree | Select-String 'postgresql|mybatis|h2|freemarker|tencentcloud-sdk-java-vrs|tencentcloud-sdk-java-tts|cos_api'
```

Expected: `git diff --check` has no errors and the dependency filter returns no lines.

- [x] **Step 8: Commit any verification-only corrections**

```powershell
git add -- src/test/java/aidenjia/digitalhuman/conversation/bailian/webrtc/BailianWebRtcDrainLifecycleTest.java conversation-ui/tests/voice-modes.test.ts src/main/java/aidenjia/digitalhuman/conversation/config/ResourceServerSecurityConfig.java
git commit -m "test: stabilize drain and voice mode cleanup checks"
```

If no correction was needed, do not create an empty commit.

## Acceptance Checklist

- [x] The backend starts and tests without any PostgreSQL/H2 configuration or driver.
- [x] Only the three retained business HTTP routes are mapped.
- [x] `/ws/bailian/realtime` is absent; `/ws/bailian/realtime-events` remains functional.
- [x] Agora join does not inject or query companion/dimension persistence.
- [x] Custom voice, voice catalog/preview, companion, dimension, scene and motion-generation code is absent.
- [x] The UI presents exactly Agora and Bailian WebRTC modes.
- [x] Existing Avatar runtime/local motion assets remain intact.
- [x] Redis-backed ownership/ticket behavior and no-Redis fallbacks remain unchanged.
- [x] README/current specs describe the reduced system; archived specs remain untouched.
- [ ] Full Maven and frontend checks pass, followed by real browser smoke tests for both retained voice paths.
