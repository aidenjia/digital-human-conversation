# Automatic Explanation Motion Implementation Plan

> Execute inline in the current feature checkout, preserving the user's existing environment and arm-pose changes.

**Goal:** Explain intent selects the existing explanation gesture automatically during an eligible spoken reply.

**Architecture:** Extend the existing strict decision schema and prompt, preload the existing rig-adapted asset, and reuse response-scoped playback scheduling and acknowledgements.

**Tech Stack:** Java 21, Spring Boot, JUnit, TypeScript, Three.js/VRM, Node test runner.

## Constraints

- One classification per response, no audio wait and no additional model calls in the runtime.
- Keep current 1500 ms expiry and 8/30-second cooldowns.
- Preserve existing arm-pose work and `.env.development`.
- Do not enable shake-head, add segment planning, or change provider voice configuration.

## Task 1: Failing contract and playback tests

- [x] Add strict explain parsing/schema tests in `AvatarDecisionSessionTest.java` and `QwenAvatarDecisionClientTest.java`.
- [x] Add explanation decision/acknowledgement/cooldown/interruption tests in `conversation-ui/tests/motion-scheduler.test.ts`.
- [x] Update `conversation-ui/tests/motion-loader.test.ts` to require wave and explain preload; keep shake-head on demand and unsupported rigs excluded.
- [x] Run `mvn -B -ntp '-Dtest=AvatarDecisionSessionTest,QwenAvatarDecisionClientTest' test` and `node --test tests/motion-scheduler.test.ts tests/motion-loader.test.ts`; confirm intended failures.

## Task 2: Implement the semantic connection

- [x] Add explain to `AvatarDecision.java` validation and schema and `avatarEvents.ts` intent list.
- [x] Extend `QwenAvatarDecisionClient.java` prompt with conservative explanation boundaries and examples.
- [x] Promote explain in `motionCatalog.ts`: label 解释, intent explain, approved, automatic, preload. Preserve rig requirements and explanation retarget strategy.
- [x] Re-run targeted tests; retain existing scheduler and session cooldown code unless a failing regression proves a change necessary.

## Task 3: Verify semantics and actual rig behavior

- [x] Extend opt-in `QwenAvatarDecisionLiveTest.java` with explanation positives, false positives and recent-explain cases.
- [x] Run the opt-in probe with existing server-side credentials without printing them; record each synthetic case and elapsed time.
- [x] Run `mvn -B -ntp clean verify`, `node --test tests/*.test.ts`, `npm run lint`, `npm run build`, and `git diff --check`.
- [x] Use ego-browser to observe the preloaded explanation asset, an automatically scheduled cue and mid-action interruption/recovery. Keep live model, browser injection and real voice evidence distinct.
- [x] Update current README/protocol documentation and this execution record with observed results.

## Observed results

- RED: five frontend failures and three backend errors rejected the unsupported explain protocol/preload behavior before implementation. Targeted GREEN: 16 frontend and 25 backend tests passed.
- Final offline backend: 177 Surefire entries (176 pass, one opt-in live class skipped) and nine integration tests, zero failures/errors. Both clean verify and final verify passed.
- Frontend: all 83 tests, lint and production build passed. Diff whitespace check passed.
- Ego Browser: wave and explain fetched before preview; explanation ready without clicking its button. Injected decision waited for synthetic audio, emitted one matching acknowledgement and visibly animated. Completion/interruption returned to idle; front/profile poses inspected. The page was reloaded after the injected checks.
- Live semantic check remains unverified: 24 cases at 1200 ms produced transport/timeouts. A single 5000 ms diagnostic request returned HTTP 403 AccessDenied with the current probe credential. Temporary HTTP diagnostic instrumentation was removed; runtime timeout and credentials were not changed.
- Remaining external acceptance: restart/update the backend and frontend together, use a credential with model access, then check real authenticated WebRTC explanation turns. No real voice-session or model-accuracy success is claimed.
