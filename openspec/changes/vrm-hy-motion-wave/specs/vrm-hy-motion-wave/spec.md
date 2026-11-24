## ADDED Requirements

### Requirement: Play the supplied HY-Motion wave
The system SHALL play the supplied generated wave on the current VRM through the existing wave command using the actual clip duration.

#### Scenario: Standing wave
- **WHEN** the wave asset is ready and the user clicks wave or invokes the voice wave action
- **THEN** the avatar raises its anatomical right hand while preserving its standing position

### Requirement: Blend and release gesture control
The system SHALL blend the clip over a procedural base and restore that base on completion or interruption.

#### Scenario: User interrupts
- **WHEN** the user interrupts or stops the gesture
- **THEN** the upper-body pose smoothly returns to idle without leaving stale clip rotations

#### Scenario: Repeated playback
- **WHEN** wave is invoked while already playing
- **THEN** the new playback starts from the displayed pose and uses its own start time

### Requirement: Preserve independent animation layers
The system SHALL leave root translation, lower-body rotations, head, eyes and finger controls outside this first imported gesture.

#### Scenario: Facial animation continues
- **WHEN** a wave plays while the avatar speaks or looks at the user
- **THEN** existing mouth, eye and head controllers retain their channels

### Requirement: Keep source assets reproducible
The system SHALL include an animation-only asset and a preparation script with source provenance.

#### Scenario: Source inspection
- **WHEN** a developer inspects the bundled wave asset
- **THEN** it contains finite quaternion keys, timing and source hash without FBX mesh or texture data
