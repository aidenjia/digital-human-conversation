# trtc-create-cloud-transcription Specification

## Purpose
TBD - created by archiving change trtc-create-cloud-transcription. Update Purpose after archive.
## Requirements
### Requirement: Start cloud transcription HTTP endpoint

The system SHALL expose `GET /api/v1/trtc/cloud-transcriptions/start` under `con.aidenjia.digitalhuman.conversation.api` with **no request body** and **no client-supplied room id**, and SHALL invoke Tencent Cloud TRTC **CreateCloudTranscription** when configuration and authentication succeed.

#### Scenario: Successful start returns StartTranscriptionData

- **WHEN** a client sends a valid authenticated request and server-side TRTC/transcription/ASR configuration is complete
- **THEN** the response is HTTP 200 JSON with envelope `ApiResponse` where `data` is **`StartTranscriptionData`** containing `sdkAppId`, `userId`, `userSig`, `strRoomId`, and `taskId` (Tencent transcription TaskId)

#### Scenario: No request body or roomId parameter

- **WHEN** a client calls the start transcription endpoint
- **THEN** the system does not require a JSON body or query parameter for `roomId`; `strRoomId` is generated server-side

### Requirement: StartTranscriptionData is a distinct response type

The system SHALL define **`StartTranscriptionData`** as a dedicated API payload type for this endpoint. The system SHALL NOT use **`StartConversationData`** as the return type for cloud transcription start, even if field names and types match.

#### Scenario: Transcription endpoint uses StartTranscriptionData

- **WHEN** the cloud transcription start endpoint returns success
- **THEN** the declared controller return type or OpenAPI schema references **`StartTranscriptionData`**, not `StartConversationData`

### Requirement: Transcription is isolated from AI conversation start

The system SHALL NOT invoke **CreateCloudTranscription** from the AI conversation start flow, and SHALL NOT invoke **StartAIConversation** from the cloud transcription start flow. The two features SHALL remain separate HTTP endpoints and separate service entry points.

#### Scenario: AI start does not start transcription

- **WHEN** a client calls `GET /api/v1/trtc/ai-conversations/start` only
- **THEN** no CreateCloudTranscription cloud API call is made

#### Scenario: Transcription start does not start AI conversation

- **WHEN** a client calls `GET /api/v1/trtc/cloud-transcriptions/start` only
- **THEN** no StartAIConversation cloud API call is made

### Requirement: Header authentication reuses TRTC filter

The system SHALL apply the existing fixed-token authentication for paths under `/api/v1/trtc/**` to the cloud transcription start route. The client TRTC UserId SHALL be parsed from the header value **`<userId>|<secret>`** as for AI conversation start.

#### Scenario: Missing or invalid token on transcription route

- **WHEN** a client calls the transcription start endpoint without the configured header or with a token that fails validation
- **THEN** the response status is 401 and no CreateCloudTranscription cloud API call is made

#### Scenario: Valid composite token sets client user for subscribe list

- **WHEN** the header value is `alice|correct-secret` and the configured fixed token is `correct-secret`
- **THEN** `alice` is used as the client TRTC UserId in the response and in `TranscriptionParam.SubscribeList`

### Requirement: Transcription bot UserId is deployment-scoped

The system SHALL set `TranscriptionParam.UserId` to a **room-scoped** transcription bot id generated as **`user_`** plus a suffix derived from the server-generated `strRoomId` (total length ≤ 32 bytes per TRTC limits). The system SHALL NOT use a single global fixed id for all concurrent transcription tasks. The system SHALL NOT use **`TRTC_ROBOT_USER_ID`** for CreateCloudTranscription.

#### Scenario: Transcription bot id is unique per start

- **WHEN** the transcription start endpoint is invoked twice for the same authenticated client
- **THEN** each outbound Tencent request uses a transcription bot `UserId` derived from that request's `strRoomId` and the two bot user ids differ when room ids differ

#### Scenario: Bot id uses user_ prefix

- **WHEN** transcription start builds the Tencent request
- **THEN** `TranscriptionParam.UserId` starts with `user_` and has length at most 32

### Requirement: Server-generated string room id

The system SHALL generate `strRoomId` as a 32-character lowercase hexadecimal string without hyphens (same generator as AI conversation start) and SHALL set `RoomIdType` to **1** (string room id) on the CreateCloudTranscription request.

#### Scenario: Room id is not client-driven

- **WHEN** any client calls the transcription start endpoint without a room id parameter
- **THEN** the outbound Tencent request uses the server-generated `strRoomId` and `RoomIdType` 1

### Requirement: ASR only without translation

The system SHALL populate **AsrParam** on CreateCloudTranscription (including at least `Lang` and `VadSilenceTime` from configuration). The system SHALL NOT set **TranslationParam** on the request in this change.

#### Scenario: No translation parameter in outbound request

- **WHEN** CreateCloudTranscription is invoked successfully
- **THEN** the request does not include TranslationParam

#### Scenario: ASR language from configuration

- **WHEN** `TRTC_ASR_LANG` (or bound property) is set to a supported value such as `16k_zh_en`
- **THEN** the outbound AsrParam uses that language value

### Requirement: Client UserSig in response uses UserSig credentials

The system SHALL return **`userSig`** in `StartTranscriptionData` generated for the **client** `userId` using `TRTC_USERSIG_SDK_APP_ID` and `TRTC_USERSIG_SECRET_KEY` via `TLSSigAPIv2`, and SHALL return **`sdkAppId`** matching that UserSig SdkAppId. The transcription bot UserSig SHALL be used only inside the Tencent API request and SHALL NOT be returned in the HTTP response body.

#### Scenario: Response contains client credentials only

- **WHEN** transcription start succeeds
- **THEN** `StartTranscriptionData.userId` and `userSig` correspond to the authenticated client user, not the transcription bot user id

### Requirement: Tencent region from configured cloud region

The system SHALL call CreateCloudTranscription using the same configured TRTC cloud region as other TRTC cloud APIs (default **`ap-guangzhou`**), which MUST be one of the regions supported by CreateCloudTranscription (ap-beijing, ap-guangzhou, ap-hongkong, ap-shanghai).

#### Scenario: Region is not client-driven

- **WHEN** any client calls the transcription start endpoint
- **THEN** the Tencent client uses the configured `trtc.cloud.region` value

### Requirement: No logging of UserSig or full sensitive payloads

The system MUST NOT write complete **UserSig** strings, cloud API secrets, or full transcription bot credentials to application logs at INFO or DEBUG for this feature. Structured logs MAY include `taskId`, `RequestId`, `strRoomId`, `sdkAppId`, transcription bot user id, and client user id.

#### Scenario: RequestId is not in response body

- **WHEN** transcription start succeeds
- **THEN** the JSON response body does not include Tencent `RequestId`; it MAY be logged server-side only

### Requirement: Stop and describe transcription APIs are out of scope

The system SHALL NOT add StopCloudTranscription, DescribeCloudTranscription, or related lifecycle HTTP routes as part of this change.

#### Scenario: No stop transcription route in this change

- **WHEN** this change is implemented
- **THEN** no new `/api/v1/trtc/**` route is documented or implemented solely for stopping or querying a cloud transcription task within this change scope

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

