# Avatar decision model

## Current protocol: automatic explanation, 2026-11-04

The current WebRTC-only implementation accepts `none/greet/agree/explain` semantic
intents with the existing emotion enum. The model chooses intent, and the local
motion catalog chooses the concrete asset. The historical gesture/PCM records
below describe earlier versions.

`explain` means the assistant is actually explaining a reason, mechanism,
distinction or steps, including a clear introductory sentence. Brief agreement,
short facts, translations, quoted examples, refusals, reassurance and explicit
physical action requests do not qualify. The existing asynchronous classification
still runs once per response and never blocks audio. Timing remains opening-only,
with a 1500 ms cue window, 8-second global cooldown and 30-second same-intent
cooldown recorded on actual playback acknowledgements.

The explanation asset is now approved for automatic selection and preloaded after
the rig's required-bone checks. Missing assets or unsupported rigs skip the gesture;
automatic playback never waits for a download. The existing rig-specific retarget,
new default side-resting arms and interruption fade remain. Shake-head is preview-only.
Restart the backend and refresh the frontend together to use the extended intent enum.

Verification: failing strict-protocol/preload/playback tests were observed before
implementation; the updated targeted tests passed. All 83 frontend tests, lint,
TypeScript/Vite build and full offline Maven verify passed. Ego Browser confirmed
preloading, an injected explain cue waiting for synthetic audio activity, a matching
playback acknowledgement, visible automatic motion, completion and interruption
returning to the idle arm pose. Event injection is not a real voice/model session.

The opt-in probe now includes 24 synthetic cases, selectable with
`AVATAR_DECISION_PROBE_CASE` (a case-label regular expression). The live run did not
validate semantic results: the 1200 ms run produced timeouts/connection errors;
a single 5000 ms diagnostic attempt exposed HTTP 403 `AccessDenied` with the current
probe credential. Production timeout/configuration was not changed. A real model
and authenticated WebRTC call remain to be checked with usable access. No accuracy
or end-to-end latency improvement is claimed.

Text-input sessions also provide user context directly without ASR. See [text input and voice reply](text-input-voice-reply.md) for usage, interruption behavior and the 2026-10-21 verification record. Gesture cooldowns and playback-start acknowledgements are unchanged.

Fixed welcome speech does not enter autonomous gesture/expression classification. Its audio, captions and response lifecycle still flow normally, and user input/transcription remains available when interrupting the welcome. This reserves the first automatic wave for a real user greeting instead of spending its cooldown before the user speaks or types. Existing gesture cooldowns still start only on actual user-response gesture playback.

Welcome cooldown regression (2026-10-21): a fresh WebRTC text session reproduced a welcome wave acknowledgement at browser time 494396.146657, followed by the first user greeting at 494404.493876 and a none decision. Both relay/control regression cases failed before the fix because welcome speech called the classifier. Both sessions now exclude welcome response events from classification, preserving audio/caption forwarding. Tests verify the first user greeting has no recent gesture and can wave; after its actual playback acknowledgement, repeated wave is still suppressed. Live recheck requires a restarted backend.

Post-fix verification: 40 focused backend tests passed, followed by a successful full offline Maven verify (including Spotless and 9 integration tests); all 40 frontend tests passed again. No live post-restart claim is made yet. Logs: `target/fixed-greeting-red.log`, `target/fixed-greeting-green.log`, `target/fixed-greeting-verify.log`.

Live restart recheck (2026-10-21) supersedes the pending note above: in Chrome WebRTC text mode, fixed welcome `resp_DIVmT1o8FHOVeOY6CHVb1` completed with no avatar.decision or gesture-start acknowledgement. The first submitted “你好” produced `wave` for `resp_DIVmNU6B6V5lLIgieSiFN` at browser time 494843.403120 and a matching avatar.gesture.started at 494843.598797. A repeated “你好” about 15.5 seconds after that actual gesture start returned `none` for `resp_DIVmRvpKykM9I8jHUNf1J`, without another gesture-start acknowledgement. This verifies first-user wave availability and retained cooldown in this real session, not a universal model success rate. The test session was ended afterward.

The conversation backend calls qwen3.7-flash asynchronously to select a gesture and expression for the assistant's opening sentence. Both PCM relay and WebRTC event sessions use the same decision session. Audio never waits for classification.

The request uses native response_format=json_schema, strict=true, required gesture/emotion fields and additionalProperties=false. Gesture choices are none, wave and nod. Expression choices are neutral, happy, sad, angry and surprised. Thinking is disabled. Responses are locally validated, including duplicate fields, unsupported values and trailing content. Invalid output is skipped rather than repaired or retried.

Configuration under digital-human.avatar-decision:

| Environment variable | Default |
| --- | --- |
| AVATAR_DECISION_ENABLED | true |
| AVATAR_DECISION_MODEL | qwen3.7-flash |
| AVATAR_DECISION_API_KEY | Reuse the resolved bailian.realtime.api-key |
| AVATAR_DECISION_BASE_URL | Derive Beijing compatible-mode/v1 from the realtime workspace; otherwise use the DashScope compatible endpoint |
| AVATAR_DECISION_TIMEOUT_MS | 1200 |

Set the base URL explicitly for another region or API gateway. Credentials remain on the server. Each eligible reply adds at most one text-model request. The timeout budget starts when classification is submitted, not when the voice response begins. It includes request queueing; expiration cancels the HTTP call. There is no automatic retry or fallback to unconstrained model output. Missing credentials or disabling the feature preserves the existing local transcript-expression behavior.

Input includes the latest user transcript (up to 1000 characters), the assistant's opening text (up to 160 characters), and the recent gesture. Submit on the first sentence terminator, at 100 characters, or on transcript/response completion for short replies. Failed/cancelled responses, barge-in, a new response, explicit action commands and disconnect cancel pending work. An explicit command also suppresses its next spoken confirmation. No conversation content, API keys or raw provider errors are logged by this adapter.

Greeting intent uses both input fields: when the user greets or says farewell to the avatar, a playful/tsundere opening can qualify for wave without a literal hello/goodbye in the opening fragment. Explicit action requests, genuine refusal and quoted/translated/third-party greetings take priority and produce none in this classifier. Recent gestures remain suppressed. No extra transcript buffering or waiting for the full reply is introduced.

Greeting probe (2026-10-20): 10 synthetic live cases matched the revised gesture expectations with a 5000 ms diagnostic budget. Rechecking with the original 800 ms budget and shared HTTP connections produced 8 valid matching results and 2 timeouts, so the default budget was raised to 1200 ms. At that budget all 10 cases passed; the related client/session/relay tests also passed (39 tests total, including the 10 live cases). This small sample does not establish a production success rate or P95. Audio does not wait for the extra budget; late/failed decisions can still be skipped. Restart the backend for prompt/config changes to take effect; an explicit AVATAR_DECISION_TIMEOUT_MS override takes precedence.

The live probe is disabled in normal test runs. To run it explicitly with a server-side DASHSCOPE_API_KEY, set AVATAR_DECISION_LIVE_TEST=true and run `mvn -Dtest=QwenAvatarDecisionLiveTest test`. Optional AVATAR_DECISION_BASE_URL and AVATAR_DECISION_PROBE_TIMEOUT_MS override the probe endpoint and deadline. The probe makes billable model calls using synthetic conversation inputs and does not print credentials or provider bodies.

Protocol regression follow-up (2026-10-20): the live semantic probe supplies userText directly and does not verify speech transcription delivery. Inspection found that the provider's input_audio_transcription.delta event uses text (with an unconfirmed stash), while the decision session and both caption relays read delta. Three new tests reproduced lost user greeting context and missing user captions. All three paths now read text, retain delta compatibility, and ignore stash. The 32 focused client/session/relay tests pass after the fix. End-to-end greeting behavior must still be checked with a restarted backend and a new voice session.

Actual greeting trace follow-up: the WebRTC trace contained an empty text delta with stash="你好", followed by speech_started; the final transcript "你好。" arrived about 214 ms after the assistant's first sentence boundary. The earlier implementation had already submitted classification without that user context. Also, a welcome-response wave suggestion was sent about 4 seconds earlier, consuming server cooldown even without confirmation of animation playback. The session now defers classification when a spoken turn has no confirmed transcript; a confirmed text delta or final/failed transcription resumes it without blocking audio. Both clients send avatar.gesture.started only when the runtime starts an automatic gesture. The server starts cooldown/recentGesture history only after a matching response/gesture acknowledgement. Expired, interrupted and superseded suggestions do not consume cooldown. The 34 focused backend tests and 31 frontend tests pass, as do frontend lint/build. Real-session recheck is pending after restart.

Full offline verify after the timing/acknowledgement fix passed: 346 unit/MVC tests plus 9 integration tests; the opt-in live test container was skipped as intended (Surefire reports 347 including that skip). Spotless and git diff --check also passed.

The server emits avatar.decision with a server-bound responseId and a 1500 ms playback window. The frontend waits for measured audio activity, applies the expression and plays a gesture only if no other gesture owns the body. Automatic gestures have an 8-second general cooldown and a 30-second cooldown for the same gesture. One decision is accepted per response. Explicit actions retain priority. In model mode local keyword-based expression inference is disabled for that response.

Animation JSON files remain in conversation-ui/public/animations and are included with the frontend build. This classifier selects existing animations; it does not generate animation assets or provide word-level synchronization.

Verification (2026-10-20): removed the duplicate AvatarDecision.NONE declaration and kept the remaining instance after the validation sets to ensure safe static initialization. Full offline Maven verify passed, including Spotless, 341 unit/MVC tests and 9 integration tests. The frontend passed full ESLint, TypeScript/Vite production build and all 30 behavior/animation tests. The build still reports large-chunk warnings. See the live checks below; precise playback synchronization, complete animation quality and target-GPU performance remain pending.

Browser/live follow-up (2026-10-20): observed actual PCM-session WebSocket frames in Chrome. Four decisions returned with matching response identities (happy, neutral, neutral and surprised; all gestures none). The first decision arrived 946 ms after response creation and 116 ms after the first received audio packet. These are browser network arrival intervals, not classifier-only duration or measured speaker playback latency. Cancelled responses and a subsequent response were observed, and an explicit dance action plus its confirmation had no autonomous decision overriding it. A manual wave preview visibly raised the character's right hand while the left arm stayed down, then returned to idle. This establishes a basic visual check, not frame-by-frame animation quality or target-GPU performance.

Live inspection also exposed a PCM microphone toggle error: calling Agora setEnabled on its deliberately muted track raised TRACK_STATE_UNREACHABLE. The toggle now controls the independent PCM stream's audio tracks and gates PCM callbacks, leaving the Agora publish track muted. Browser recheck confirmed no audio.append frames while disabled and resumed audio.append frames after enabling. Frontend ESLint, production build and all 30 tests passed again after this fix.

WebRTC live follow-up: session.ready, response lifecycle/transcript events and a matching avatar.decision (none/neutral) were received. A nod action and its spoken confirmation were also observed without an autonomous decision on the explicit action or confirmation. Microphone disable/enable and session teardown succeeded in the UI. Both test sessions were ended after verification. These checks establish connectivity, event delivery and basic visual behavior; they do not measure audible output timing, perceptual lip synchronization, sustained session stability or GPU frame rate.
