## ADDED Requirements

### Requirement: Cloud transcription uses StartAITranscription via TrtcClient

The system SHALL invoke Tencent Cloud TRTC **`StartAITranscription`** through **`com.tencentcloudapi.trtc.v20190722.TrtcClient`** (typed SDK models). The system SHALL NOT invoke **`CreateCloudTranscription`** via `CommonClient` or custom `trtc.cloudapi` request classes in the transcription start flow.

#### Scenario: Successful start calls StartAITranscription

- **WHEN** `TrtcCloudTranscriptionService#startTranscription` runs with valid configuration and authentication context
- **THEN** the service calls `TrtcClient.StartAITranscription` with `StartAITranscriptionRequest` and returns the SDK `TaskId` in `StartTranscriptionData`

#### Scenario: No NoSuchFieldException from SDK serialization

- **WHEN** the transcription start flow serializes the outbound request
- **THEN** the SDK uses official `StartAITranscriptionRequest` models without `java.lang.NoSuchFieldException` for fields such as `SdkAppId` or `RoomId`

### Requirement: HTTP and response contract unchanged

The system SHALL keep **`GET /api/v1/trtc/cloud-transcriptions/start`**, Header authentication (`<clientUserId>|<fixedToken>`), no request body, server-generated `strRoomId`, and response type **`StartTranscriptionData`** (`sdkAppId`, `userId`, `userSig`, `strRoomId`, `taskId`).

#### Scenario: Endpoint and payload unchanged for clients

- **WHEN** a client calls the transcription start endpoint with valid auth
- **THEN** the HTTP method, path, and `StartTranscriptionData` field names remain as before this change

### Requirement: TranscriptionParams and RecognizeConfig mapping

The system SHALL build **`TranscriptionParams`** with transcription bot `UserId` = `user_` + truncated room id suffix (fixed prefix, max total length 32), bot `UserSig` from `TLSSigAPIv2`, **`TargetUserId`** = authenticated client user id, **`MaxIdleTime`** and **`TranscriptionMode`** from `classpath:trtc/transcription-param.json`. The system SHALL build **`RecognizeConfig`** with **`Language`** and **`VadSilenceTime`** from `trtc.asr` configuration. The system SHALL NOT configure translation on the request.

#### Scenario: Client user is transcription target

- **WHEN** the auth header resolves to client user `alice`
- **THEN** `TranscriptionParams.TargetUserId` is `alice` and response `userId` / `userSig` are for `alice`

#### Scenario: RecognizeConfig carries ASR settings only

- **WHEN** StartAITranscription is invoked
- **THEN** `RecognizeConfig` includes configured language and VAD silence time and does not enable translation parameters

### Requirement: Isolation from AI conversation API

The transcription start flow SHALL NOT call **`StartAIConversation`**, and the AI conversation start flow SHALL NOT call **`StartAITranscription`**.

#### Scenario: Transcription start does not start AI conversation

- **WHEN** only the cloud transcription start endpoint is invoked
- **THEN** no `StartAIConversation` cloud API call is made

### Requirement: Remove cloudapi bridge components

The codebase SHALL remove **`TrtcCloudTranscriptionApi`** and the **`con.aidenjia.digitalhuman.conversation.trtc.cloudapi`** package used solely for CreateCloudTranscription bridging.

#### Scenario: No CommonClient transcription call

- **WHEN** the application is built after this change
- **THEN** no production code path calls `CommonClient.commonRequest` for transcription start

### Requirement: Shared TrtcClient factory and error mapping

The system SHALL obtain `TrtcClient` via **`TrtcCloudClientFactory`**. On **`TencentCloudSDKException`**, the system SHALL throw **`TrtcUpstreamException`**. On missing TRTC/ASR configuration, the system SHALL throw **`TrtcConfigurationException`** before calling the cloud API.

#### Scenario: Upstream failure surfaces as TrtcUpstreamException

- **WHEN** `StartAITranscription` fails with `TencentCloudSDKException`
- **THEN** the service throws `TrtcUpstreamException` and does not return success `StartTranscriptionData`
