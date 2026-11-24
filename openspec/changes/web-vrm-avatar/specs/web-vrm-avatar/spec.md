## ADDED Requirements

### Requirement: Local VRM stage
The realtime client SHALL support locally importing a VRM character and SHALL provide a placeholder when no model is selected.

#### Scenario: User imports a supported model
- **WHEN** the user selects a valid VRM file smaller than 50 MB
- **THEN** the client loads its scene, normalized humanoid bones and available expressions
- **AND** replacing or unloading the model releases its resources

### Requirement: Playback-driven mouth
The client SHALL derive amplitude mouth motion from the media playback path, not network packet arrival.

#### Scenario: AI audio plays
- **WHEN** scheduled PCM or remote WebRTC audio is active
- **THEN** the available aa expression follows a noise-gated and smoothed amplitude estimate
- **AND** analysis SHALL NOT introduce a second audible output

### Requirement: Interruption and actions
The client SHALL coordinate local motions and mouth state with existing realtime events.

#### Scenario: User interrupts
- **WHEN** speech.started is received
- **THEN** the avatar closes its mouth and enters listening state
- **AND** it cancels the active gesture
- **AND** the existing audio cancellation path remains in use

#### Scenario: An action arrives while PCM audio plays
- **WHEN** the client receives an action event
- **THEN** supported avatar effects execute without flushing the PCM playback queue

### Requirement: Lightweight rendering
The stage SHALL avoid React state updates for frame data and SHALL cap rendering at 60 FPS in sharp mode and 30 FPS in smooth mode.

#### Scenario: Device cannot render at the cap
- **WHEN** frame processing takes longer than the frame budget
- **THEN** animation transitions remain time based
- **AND** the user can hide the stage while retaining conversation controls

### Requirement: Response-scoped conversational cues
The realtime relays SHALL forward response lifecycle identifiers and preserve response identity on assistant transcripts. The client SHALL discard cues for interrupted responses and expire cues that never reach playback.

#### Scenario: Late emotion after barge-in
- **WHEN** an expression belongs to a response interrupted by user speech
- **THEN** that expression SHALL NOT become active again, including after a new response starts

#### Scenario: Assistant cue before playback
- **WHEN** an explicit phrase matches a conservative expression rule
- **THEN** the client SHALL queue at most one inferred expression per response
- **AND** activate it only during detected audio playback or discard it after 5 seconds without playback

### Requirement: State-dependent natural gaze
Natural idle behavior SHALL be enabled by default and SHALL use listening, thinking, speaking and idle states to control gaze dwell and bounded target offsets. Users SHALL be able to disable natural idle for static comparison.

#### Scenario: Audio pauses briefly
- **WHEN** speaking audio pauses for less than 350 ms
- **THEN** the avatar SHALL remain in speaking state

#### Scenario: Response stalls
- **WHEN** no playback arrives for 8 seconds after entering thinking state
- **THEN** the avatar SHALL return to idle instead of remaining stuck
