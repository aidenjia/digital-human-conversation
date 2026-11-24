# Imported wave animation

## Automatic explanation, 2026-11-04

`explain` is now an approved, preloaded automatic asset mapped to semantic intent
`explain`. It uses the existing rig-proportioned explanation retarget and new
side-resting idle. The model must be explaining reasons, mechanisms or steps, not
merely acknowledging, translating or comforting. Playback still waits for measured
audio activity and obeys the existing cue expiry, interruption and intent cooldowns.
Shake-head remains on demand and preview-only. Historical trial-only notes below
are superseded for explanation; see `docs/avatar-decision.md` for verification limits.

## Default side-resting arms, 2026-11-04

The previous 0.22-radian forward upper-arm inclination left both hands visibly
reaching in front of the dress after a wave. The shipped default model now uses
a 0.04-radian forward inclination and a 0.31 lateral slope, placing the hands
beside the skirt with neutral forearms and the existing relaxed fingers. Imported
models retain their previous idle and full wave retargeting.

For this default model, wave playback uses the source right shoulder/arm/wrist
tracks while the torso and left arm keep their procedural idle. Blending those
source tracks from a different forward-resting pose crossed the skirt during
entry and recovery. The right-arm-only strategy preserves the actual wave and
returns to the new idle without that whole-body transition.

Regression checks cover completion/interruption endpoints, bounded forward reach
and lateral spread, at least 8 mm distance from sampled idle wrist/finger points
to the actual skirt surface, and 60 Hz hand/finger-segment intersection sampling
through wave and explanation playback. This is not whole-hand mesh collision or
arbitrary-garment validation. Ego Browser front/profile wave previews and endpoint
bone comparisons were checked locally; perceived naturalness remains subjective.

## Relaxed default hand pose, 2026-10-22

The default dress model now uses a small static finger curl toward its own palm
instead of leaving all four fingers in the straight bind pose while arms hang.
The curl uses the actual palm/finger basis on both hands, is idempotent, and
does not change upper-arm position, neutral forearms, wrists or the accepted
dress-specific forward inclination. Imported models are not assigned this pose.
Gesture clips still do not animate finger tracks; explanation's palm adaptation
is baked from this updated idle. Keep the full lowering/skirt regression.

For local visual review without a voice session, run Vite and open
/tools/avatar-preview.html. It renders the same AvatarStage and local assets;
it does not create voice sessions or access protected backend data.

## Runtime dispatch update, 2026-10-22

Semantic decisions now carry intent (none/greet/agree), responseId, cueId,
segmentId and expiry. Concrete IDs belong to the frontend motion catalog.
The model does not receive the asset list. New variants in an existing intent
only require a prepared JSON and a catalog entry; no new model enum is needed.

The catalog declares source, admission, automatic eligibility, required bones,
load policy, priority and retarget strategy. Only wave preloads. Shake-head and
explanation remain preview-only and load on click; failure can be retried.
Do not mark a new asset approved without complete actual-rig visual acceptance.
Changing an action's ID does not automatically apply explanation adaptation:
set retargetStrategy to explanation for those variants.

The response-scoped scheduler accepts independent cue IDs, discards duplicates,
expired cues and cues for passed speech segments, and queues busy-body cues
only until expiry. Existing 8-second global and 30-second per-intent cooldowns
remain. Production still emits one opening cue per response; dense sequences
and real playback-segment synchronization are deferred.

Acknowledgements include cueId, intent and concrete motionId and are sent only
on actual start. WebRTC and PCM use the same shape. Deploy frontend and backend
together and refresh existing pages: legacy gesture events are unsupported.

Full design: docs/superpowers/specs/2026-10-22-avatar-motion-dispatch-design.md.

Source: user-provided TokenHub HY-3D-Motion FBX, motion-1bdf34d6-68b7-4a81-94a6-f41364afa51f.fbx.
SHA-256: 74d723c46dcbbf003fc8ef935b06a92ac73bcd34b4996e18b33564464845b647.

The original FBX stays in the user's Downloads directory. This export contains no source mesh or textures.
Duration: 2.966667 seconds; 90 frames at approximately 30 Hz; 11 upper-body rotation channels; 41,678 bytes.

Reproduce from conversation-ui:

~~~powershell
node tools/prepare-hy-motion.mjs 'C:/path/to/motion.fbx' public/animations/wave.motion.json
~~~

The runtime converts canonical source rotations to normalized VRM axes. Optional chest/shoulder joints are composed into available parent/arm joints.
Root translation, hips/legs, template finger poses, head and gaze are deliberately excluded from this first standing gesture.

## Conversation library (WebRTC primary)

`src/avatar/motionCatalog.ts` lists prepared assets for preview. The runtime uses a single
multi-clip player with a shared set of owned bones, continuous switching and
interruption recovery. Each loaded action registers its own duration. Unprepared
new actions cannot execute. The catalog includes the accepted wave and two newly
generated previews (shake_head and explain). Semantic admission remains separate.

Export another prepared HY-Motion FBX using an explicit action name:

~~~powershell
node tools/prepare-hy-motion.mjs 'C:/path/to/motion.fbx' public/animations/explain.motion.json explain
node tools/prepare-hy-motion.mjs 'C:/path/to/motion.fbx' public/animations/shake_head.motion.json shake_head --include-head
node tools/prepare-hy-motion.mjs 'C:/path/to/motion.fbx' public/animations/shake_head.motion.json shake_head --head-only
~~~

`--include-head` requires `Neck` and `Head` quaternion tracks. Verify these names
on the source rig. Head and neck rotations are optional for other gestures.
The generic format accepts only the existing upper-body channels plus head/neck;
it rejects root/leg/finger channels. Check head/gaze interaction on the actual VRM.

Admission order: source provenance and export, actual VRM preview, transition and
interruption checks, catalog entry, then backend/client semantic whitelist.
Do not enable a semantic action solely because its name exists in the planned type.

## Fresh shake-head preview, 2026-10-21

Only the previously accepted wave is reused. All other old local FBX files are excluded.
New source: `data/motion/4475eaf9-6348-4135-ad2f-906b1145e3f6.fbx`.
Prompt: `Face forward, arms hanging at sides, feet still. Turn head left, right, left, right to say no, then face forward.`
Requested 3 seconds; export 2.966667 seconds, 90 frames, 2 head/neck channels,
8,381 bytes. Export with `--head-only` to preserve idle arms and exclude unrelated
source body posing. Source hash is embedded in the exported JSON.
The actual VRM regression proves turning both ways, untouched arms and recovery.
Browser preview is exposed as `摇头`. It is preloaded and eligible for automatic
selection for the `deny` semantic intent; actual conversation classification and
subjective motion quality still require live verification.
The first newly generated attempt was rejected; see `tools/motion-generation-results.json`.

## Fresh explanation preview, 2026-10-21

New source: `data/motion/41668085-99ec-4346-806b-81451858f549.fbx`.
Prompt: `Stand with feet planted. Open both hands at waist height, palms up, gently explain, then lower both arms to rest.`
Requested 4 seconds; export 3.966667 seconds, 120 frames, 11 upper-body channels,
55,311 bytes. Chrome WebRTC preview shows both forearms opening with palms up
and subsequent idle restoration. Actual-rig regression verifies bilateral hand
raise and arm recovery. Full-clip visual acceptance remains pending; semantic
selection is disabled. Never reuse the other old local FBX files.

## Adaptation revision after user feedback

The initial previews above were not visually accepted by the user: shake_head
was too slow and explain had incorrect arm placement. Do not treat the earlier
browser observations or hand-raise tests as quality acceptance.

Shake-head is now trimmed to source 0.35–2.4 seconds and retimed to 1.35 seconds
(42 samples). Reproduce with `node tools/retime-shake-head.mjs` from conversation-ui;
it always reads the new source FBX, never an already-retimed export.

Explanation now applies `explanationRetarget.ts` during clip creation for each rig.
Generated elbow flexion supplies the activation curve, smoothed over time. An
analytic two-bone solve places wrists in front of the mid-torso and elbows beside
the body. The actual finger-fan basis constrains palms up and fingers outward;
source wrist twist is not copied. Solved rotations are smoothed offline to avoid
extension-related angular spikes. This is a calibrated adaptation of the generated
motion, not a claim that the raw generated arm trajectory was suitable.

Regression covers hand height, forearm direction, elbow depth, palm orientation,
arm lengths 0.85/1/1.15, scene yaw/scale, and all arm/wrist angular steps below
8 degrees per 60 Hz sample during entry/recovery. These are engineering gates,
not visual or frame-rate acceptance. No IK runs in the per-frame playback path.

Chrome temporarily became unavailable, then reconnected. Follow-up on this
revision: front preview showed raised forearms and upward-facing palms near the
mid-torso; side preview showed the hands in front of the body, without obvious
intersection in the inspected poses. Mid-action stop restored idle. Retimed
shake-head was replayed, and the page was returned to portrait/front framing.
52 frontend tests, full ESLint and TypeScript/Vite build pass (existing chunk
advisory remains). These sampled browser observations are not full-frame collision
analysis or final subjective acceptance. Both buttons retain trial labels and
automatic semantic selection remains disabled.

## Full-path recovery revision after further user feedback

The user reported incorrect entry, skirt intersection after lowering, and a
disconnected final recovery. Investigation found that the old default finger
positions were inside the actual dress horizontal envelope; additionally the
calibrated clip retained torso/shoulder offsets after the hand gesture ended,
then performed a second return during the player's terminal fade.

That revision kept upper arms close but added an outward forearm rotation for
skirt/fingertip clearance (subsequently rejected and removed; see below).
Explanation lift/lower uses an outward wrist
arc, source torso/shoulder offsets settle with gesture activation, and near-rest
arm/wrist offsets converge to the exact idle pose. Every track starts and ends
in idle, so playback fade does not introduce another separate recovery.

`tests/fixtures/vrm-garment.ts` CPU-skins the shipped VRM's dress vertices using
actual joint weights and inverse bind matrices. Moving sleeves are excluded from
the body/skirt envelope. Tests check wrists and distal finger points against dress
horizontal bounds, per-frame displacement, late settling and exact endpoints.
This is a conservative sampled envelope test, not general triangle/capsule collision
detection or a guarantee for arbitrary clothing.

55 frontend tests, full ESLint and production build pass. Chrome replay covered
front entry, open pose, lowering and several seconds after return; side lowering
was also sampled. Final browser pose is back to portrait/front. The inspected
poses no longer show the previously observed hand/skirt crossing or second late
arm return. Trial labels remain for subjective user acceptance.

## Natural idle restoration after user feedback

The user preferred the original relaxed arm posture. Removed the global 0.26-radian
forearm rotation from both the production loader and actual-rig fixture; upper-arm
idle remains unchanged. Garment avoidance is confined to the explanation wrist arc.
The explanation clip still starts and ends exactly at the restored natural idle,
with no separate late recovery. Only entry smoothing is slightly stronger (150 ms,
returning to the prior 140 ms by 0.9 seconds) to accommodate the longer initial reach.

The restored idle overlaps the conservative skirt horizontal envelope. This alone
does not establish real mesh penetration. The envelope test now explicitly checks
that explanation does not worsen each accepted idle point's envelope clearance
(2 mm sampling tolerance), rather than requiring a different global idle. This is
a baseline-relative regression gate, NOT a claim of absolute garment clearance.
Angular/displacement, palm/elbow placement, proportion and exact-endpoint gates
are retained. Final clothing and motion appearance still require user acceptance.

Fresh validation: 55 frontend tests pass, ESLint and production build pass (existing
large-chunk advisory). Chrome WebRTC preview confirmed the restored front idle,
sampled explanation entry and return to the same idle. No full-frame collision
verification is claimed. Trial labels and disabled automatic selection remain.

## Actual skirt-surface correction after lowering feedback

The user confirmed improved naturalness but still saw intersection while lowering.
The previous baseline-relative envelope gate was insufficient: a new CPU-skinned
triangle/hand-finger-segment test reproduced crossings around 2.02–2.12 seconds
(and a few during entry). Rays also showed that the original resting hand/finger
points were inside the actual skirt, so preserving that exact endpoint could not
guarantee a collision-free return.

Historical calibration (superseded by the 2026-11-04 update above): for the shipped
default dress only, upper arms inclined forward by 0.22 radians;
their lateral angle remains unchanged and normalized forearms remain neutral.
Imported models do not receive this dress-specific inclination. This moves the
return endpoint forward instead of spreading the arms sideways. Explanation's
existing rig-proportioned bake uses that endpoint without a separate late return.

New checks cover wrist/finger-point forward skirt clearance (8 mm in default idle)
and no hand-to-index/middle/little finger bone-segment intersection with actual
skirt triangles at 60 Hz through entry, lowering and recovery. These tests are NOT
a whole-hand skin-volume collision solver; sleeves, secondary cloth physics and
arbitrary garments are not covered. There is no per-frame collision/IK overhead.

57 frontend tests, ESLint and production build pass (existing chunk advisory).
Chrome WebRTC front and side lowering/idle poses were sampled; hands remain in
front of the skirt in those views. Camera returned to front portrait. Naturalness
and full visual acceptance remain with the user; automatic selection stays off.
