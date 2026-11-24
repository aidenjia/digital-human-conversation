# Conversation Motion Library Implementation Plan

> Execute inline in this session, task by task. User approved the motion-library design and selected WebRTC as the primary transport.

**Goal:** Build a reusable, asset-driven conversational motion library and validate it through WebRTC.

**Architecture:** Generalize the existing validated wave pipeline into one player that owns the union of imported rotation tracks. Admit each new real asset to semantic selection only after preview acceptance.

**Tech Stack:** TypeScript, Three.js, three-vrm, Node test runner, Spring Boot.

## Global Constraints

- WebRTC is the development and live acceptance path.
- Audio must not wait for assets or gesture classification.
- Preserve shipped wave asset behavior and independent facial animation.
- No invented/generated-in-code replacement for a claimed imported animation.
- New semantic actions require ready and visually accepted assets.
- Reuse only the accepted wave; exclude every other existing local FBX. Generate all new motions afresh (user correction, 2026-10-21).

### Task 1: General motion data and player

Files: `conversation-ui/src/avatar/WaveAnimation.ts`, `conversation-ui/src/avatar/AvatarRuntime.ts`, `conversation-ui/tests/motion-animation.test.ts`.

- [x] Write tests importing `parseMotion`, `MotionAnimation`; create a second named rotation clip from the existing fixture to test mechanics, explicitly not a new production asset.
- [x] Run `node --test tests/motion-animation.test.ts`, confirm missing API fails.
- [x] Add a strict name/bone-bounded parser and a player accepting multiple clips. Preserve compatibility wrappers `parseWaveMotion`, `createWaveClip`, `WaveAnimation`.
- [x] Test switching a head-only clip and wave from displayed poses, restoration of outgoing arm tracks, interruption and forbidden bones. Run existing wave tests.

### Task 2: Asset export and WebRTC preview admission

Files: `conversation-ui/tools/prepare-hy-motion.mjs`, `conversation-ui/src/avatar/AvatarStage.tsx`, `conversation-ui/public/animations/README.md`.

- [x] Inspect existing local FBX files and hashes before generating new assets.
- [x] Generalize export name and optional head/neck inclusion with compatibility for the current wave conversion.
- [x] Replace wave-specific scene integration with a loaded motion catalog and single player; expose only loaded accepted motions.
- [x] Run `node --test tests/*.test.ts`, `npm run lint`, `npm run build`.

### Task 3: Produce and admit conversational assets

Generation input: `conversation-ui/tools/conversation-motion-batch.json`, eight fresh clips.
Each prompt must fit the current 128-character API constraint; duration is 1–12 integer seconds.
Start with one new clip and inspect the result before submitting the rest.

- [ ] Prepare eight high-frequency actions in the approved order. Record source, prompt, hash, duration, channels and visual acceptance for each asset.
- [ ] Preview each action on the actual VRM, checking body participation, transitions, collision and foot stability.
- [ ] Extend backend/client semantic enums together for accepted assets, including acknowledgement and cooldown tests.
- [ ] Validate actual WebRTC conversations, interruption and latency regression. Track unready assets as pending, never as completed gestures.

## Execution evidence, 2026-10-21

Tasks 1 and 2 implemented. Frontend: 45/45 tests pass, full ESLint passes,
TypeScript/Vite production build passes with the existing large-chunk advisory.
`git diff --check` passes. No backend code or paid generation calls changed.
Browser/live regression of the updated player remains pending.

Local source inventory: two additional 150-frame FBX clips (4.9667 seconds)
and one additional 90-frame FBX clip (2.9667 seconds) parse through the converter.
Their semantic content and visual quality have not been established. The exporter
reports source mesh weight warnings; those meshes are excluded from motion output.
No additional asset was admitted to the production catalog. Task 3 remains open.
The user subsequently excluded all these additional local files; this inventory
is historical inspection evidence only, not a candidate list. Do not inspect or
reuse them further. All new actions must be generated afresh.

Fresh-generation progress: shake_head attempt 1 rejected; attempt 2 exported as
head/neck-only preview. Explain attempt 1 generated, downloaded and exported as
an upper-body preview. Both are available on the WebRTC page with trial labels,
with source/task provenance in `conversation-ui/tools/motion-generation-results.json`.
Actual-rig tests cover both-direction head rotation without arm ownership and
bilateral hand raise with arm restoration. Six other first-batch gestures remain
ungenerated; semantic admission and live response-trigger testing remain open.

User rejected both new initial previews. Follow-up: shake_head retimed to 1.35s;
explain now bakes rig-proportioned arm IK and palm alignment during loading.
Added stricter geometry and temporal gates instead of relying on hand raise alone.
The implementation is still pending live front/side Chrome acceptance because
the Chrome control inventory became empty during this revision. Do not declare
these assets accepted or enable automatic semantic selection yet.

Follow-up after Chrome reconnected: inspected updated explanation from front,
back and side, and verified mid-action stop restores idle. No obvious intersection
in inspected poses; hands now in front near mid-torso. Replayed retimed shake-head
and restored original portrait/front camera. 52 tests, full lint and build pass.
Final subjective user acceptance and actual conversation-trigger testing remain
open. No new paid generations were submitted for this adaptation revision.
