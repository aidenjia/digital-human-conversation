## MODIFIED Requirements

### Requirement: Transcription bot UserId is deployment-scoped

The system SHALL set `TranscriptionParam.UserId` to a **room-scoped** transcription bot id generated as **`user_`** plus a suffix derived from the server-generated `strRoomId` (total length ≤ 32 bytes per TRTC limits). The system SHALL NOT use a single global fixed id for all concurrent transcription tasks. The system SHALL NOT use **`TRTC_ROBOT_USER_ID`** for CreateCloudTranscription.

#### Scenario: Transcription bot id is unique per start

- **WHEN** the transcription start endpoint is invoked twice for the same authenticated client
- **THEN** each outbound Tencent request uses a transcription bot `UserId` derived from that request's `strRoomId` and the two bot user ids differ when room ids differ

#### Scenario: Bot id uses user_ prefix

- **WHEN** transcription start builds the Tencent request
- **THEN** `TranscriptionParam.UserId` starts with `user_` and has length at most 32

### Requirement: Tencent Java SDK supports CreateCloudTranscription

The system SHALL declare `com.tencentcloudapi:tencentcloud-sdk-java-trtc` and `tencentcloud-sdk-java-common` at a version that includes **CreateCloudTranscription** API models and **`TrtcClient.CreateCloudTranscription`**, verified by successful compilation and `mvn -q verify`. The project MAY use **`LATEST`** coordinates until a pinned version is adopted.

#### Scenario: Reproducible SDK with CreateCloudTranscription

- **WHEN** a developer runs `mvn -q verify` after resolving TRTC SDK dependencies
- **THEN** `TrtcClient.CreateCloudTranscription`, `CreateCloudTranscriptionRequest`, and related model classes resolve without error

#### Scenario: Production code uses TrtcClient only

- **WHEN** the application is built after this change
- **THEN** transcription start calls `TrtcClient.CreateCloudTranscription` and does not call `CommonClient.commonRequest` or classes under `trtc.cloudapi`

### Requirement: Secrets use environment not repository plaintext

The system SHALL read `TRTC_CLOUD_SECRET_ID`, `TRTC_CLOUD_SECRET_KEY`, `TRTC_SDK_APP_ID`, `TRTC_USERSIG_SDK_APP_ID`, `TRTC_USERSIG_SECRET_KEY`, ASR settings, and `API_FIXED_TOKEN` from environment or externalized configuration. The repository SHALL NOT require committing production secrets for this feature. Transcription bot UserId SHALL be generated server-side and SHALL NOT require **`TRTC_TRANSCRIPTION_USER_ID`**.

#### Scenario: Env templates omit obsolete transcription user id

- **WHEN** inspecting committed `.env` templates for this feature
- **THEN** documentation does not require `TRTC_TRANSCRIPTION_USER_ID` for transcription bot identity (room-scoped `user_` ids are generated per start)
