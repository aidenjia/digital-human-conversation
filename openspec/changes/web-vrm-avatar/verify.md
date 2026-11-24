# Verification

- PASS: `node --test tests/avatar-runtime.test.ts` — 4 tests, covering interruption/resume, teardown reset, gesture lifetime, unknown commands and time-based smoothing.
- PASS: ESLint on changed client files and the new tests.
- PASS: `git diff --check`.
- PASS: `openspec validate web-vrm-avatar --strict` (CLI telemetry failed to reach its server, without affecting validation).
- BLOCKED: installing dependencies from npm and obtaining a sample asset from GitHub — DNS resolution fails (`ENOTFOUND`).
- BLOCKED: production TypeScript / Vite build — Three.js, R3F, three-vrm and their type declarations are not installed. Their downstream typing has not been verified.
- PENDING: regenerate package-lock.json using npm install with network access.
- PENDING: real VRM rendering, WebRTC / PCM conversations, barge-in timing and 780M performance checks.

The change is not ready to archive as a fully validated prototype.

## Default character integration (2026-10-16)

- Imported the user-provided official AvatarSample_2 as public/models/avatar-sample-2.vrm (17,936,516 bytes), preserving the binary and source/license information.
- Verified GLB structure, 54 humanoid bones, embedded textures, mouth/blink/emotion bindings, and absence of external asset URIs.
- Default model loads automatically; local replacement and return-to-default controls remain available.
- PASS: npm run build, including TypeScript and Vite production bundling. Dependencies and lockfile are now installed/updated, superseding the earlier DNS blocker.
- PASS: ESLint on AvatarStage.tsx.
- BLOCKED: automated browser rendering check; Chrome returned net::ERR_NETWORK_ACCESS_DENIED for the local preview URL. No visual quality or live conversation claim is made.
- PENDING: real browser rendering, mouth synchronization, gesture appearance and target GPU performance.

## Clarity adjustment (2026-10-16)

- User reported a blurry avatar at localhost:5173. Browser access remains blocked by net::ERR_NETWORK_ACCESS_DENIED, so the reported appearance has not been directly observed.
- Static inspection found fixed DPR 1 and disabled antialiasing in the initial low-power configuration. These are plausible contributors to reduced edge quality and high-DPI softness.
- Enabled MSAA and a default sharp profile with DPR 1.5–2; added a smooth profile with DPR 1 to allow lower GPU cost. Kept the 30 FPS cap and existing camera.
- Binary inspection confirmed face color texture 1024×1024, body/clothing 2048×2048, and no new texture compression or blur pass.
- Pending user-side visual comparison and target GPU performance validation.

## Resting arms and portrait framing

The supplied VRM 0.x has left arm direction X < 0 and right arm X > 0. Initial fixed rotations raised both elbows by about 0.166 world units. Resting rotations now derive from normalized lower-arm offsets; gesture offsets mirror with the actual axis. Regression tests cover the real sample and opposite-axis rigs. Camera distance changed from 2.7 to 1.65 with a 1.35 look target, and canvas height increased for a half-body view. Automated browser inspection remains unavailable; visual confirmation is pending.

## Static presentation baseline

Default idle motion is now disabled for material comparison; speech mouth, explicit expressions and intentional gestures remain available. Canvas flat keeps sRGB output while disabling ACES tone mapping. Neutral background, ambient 0.65, key light 1.1 and fill light 0.25 provide a lower-intensity starting baseline; these are not verified VRoid Hub settings. Portrait/full framing and bounded wheel zoom are available. Sharp mode renders at up to 60 FPS and smooth mode at up to 30 FPS. Browser visual matching and 780M performance validation remain pending.

## Conversation behavior iteration (2026-10-19)

Implemented response lifecycle events and transcript responseId on both realtime transports. Added local idle/listening/thinking/speaking estimation, gaze dwell/short aversion, conservative phrase-triggered expressions gated by playback, and cancellation/expiry. Added voice wave/nod commands. User barge-in clears gestures and expression cues; the existing tool-induced speech cancellation preserves the tool's independent gesture/expression.

- PASS: 15 Node behavior/pose tests.
- PASS: TypeScript and Vite production build; ESLint on changed client files and tests.
- PASS: 14 targeted relay tests (both transports), including response identity/cancellation.
- PASS: full offline Maven verify, including integration tests, using an explicit startup Byte Buddy agent solely for this test run.
- PASS: OpenSpec strict validation and git diff --check.
- Initial plain Maven test stalled because Mockito dynamic attach could not open its named pipe in this environment. Startup instrumentation avoided this issue without changing repository test configuration.
- No new model calls or paid API requests were made.
- PENDING: live browser rendering, real voice barge-in and 780M performance. Precise visemes and FBX/retargeted animation assets are not part of this iteration.
