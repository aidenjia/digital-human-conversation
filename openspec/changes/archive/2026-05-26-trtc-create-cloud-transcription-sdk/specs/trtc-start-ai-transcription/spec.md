## REMOVED Requirements

### Requirement: Cloud transcription uses StartAITranscription via TrtcClient

**Reason**: Product requires **CreateCloudTranscription**; typed SDK now exposes `TrtcClient.CreateCloudTranscription` without the former `CommonClient` / `cloudapi` bridge.

**Migration**: Implement `TrtcCloudTranscriptionService` with `CreateCloudTranscriptionRequest`, `TranscriptionParam`, and `AsrParam`. HTTP contract and `StartTranscriptionData` remain unchanged.

### Requirement: TranscriptionParams and RecognizeConfig mapping

**Reason**: Replaced by **TranscriptionParam** + **AsrParam** on CreateCloudTranscription.

**Migration**: Map `SubscribeList` to authenticated client user id; map ASR settings to `AsrParam.Lang` and `AsrParam.VadSilenceTime`.

## ADDED Requirements

### Requirement: Cloud transcription uses CreateCloudTranscription via TrtcClient

The system SHALL invoke Tencent Cloud TRTC **`CreateCloudTranscription`** through **`com.tencentcloudapi.trtc.v20190722.TrtcClient`** using official SDK model classes (`CreateCloudTranscriptionRequest`, `CreateCloudTranscriptionResponse`, `TranscriptionParam`, `AsrParam`). The system SHALL NOT invoke **`StartAITranscription`** in the transcription start flow. The system SHALL NOT invoke **`CreateCloudTranscription`** via `CommonClient` or custom `trtc.cloudapi` request classes.

#### Scenario: Successful start calls CreateCloudTranscription

- **WHEN** `TrtcCloudTranscriptionService#startTranscription` runs with valid configuration and authentication context
- **THEN** the service calls `TrtcClient.CreateCloudTranscription` and returns the SDK `TaskId` in `StartTranscriptionData`

#### Scenario: No NoSuchFieldException from SDK serialization

- **WHEN** the transcription start flow serializes the outbound request
- **THEN** the SDK uses official `CreateCloudTranscriptionRequest` models without `java.lang.NoSuchFieldException` for fields such as `SdkAppId` or `RoomId`

### Requirement: TranscriptionParam and AsrParam mapping

The system SHALL build **`TranscriptionParam`** with transcription bot `UserId` = `user_` + truncated room id suffix (fixed prefix, max total length 32), bot `UserSig` from `TLSSigAPIv2`, **`SubscribeList`** containing the authenticated client user id, **`MaxIdleTime`** and **`SendCustomMode`** from `classpath:trtc/transcription-param.json`. The system SHALL build **`AsrParam`** with **`Lang`** and **`VadSilenceTime`** from `trtc.asr` configuration. The system SHALL NOT set **TranslationParam** on the request.

#### Scenario: Client user is in SubscribeList

- **WHEN** the auth header resolves to client user `alice`
- **THEN** `TranscriptionParam.SubscribeList` includes `alice` and response `userId` / `userSig` are for `alice`

#### Scenario: AsrParam carries ASR settings only

- **WHEN** CreateCloudTranscription is invoked
- **THEN** `AsrParam` includes configured language and VAD silence time and the request does not include translation parameters

## MODIFIED Requirements

### Requirement: Isolation from AI conversation API

The transcription start flow SHALL NOT call **`StartAIConversation`**, and the AI conversation start flow SHALL NOT call **`CreateCloudTranscription`**.

#### Scenario: Transcription start does not start AI conversation

- **WHEN** only the cloud transcription start endpoint is invoked
- **THEN** no `StartAIConversation` cloud API call is made

### Requirement: Shared TrtcClient factory and error mapping

The system SHALL obtain `TrtcClient` via **`TrtcCloudClientFactory`**. On **`TencentCloudSDKException`**, the system SHALL throw **`TrtcUpstreamException`**. On missing TRTC/ASR configuration, the system SHALL throw **`TrtcConfigurationException`** before calling the cloud API.

#### Scenario: Upstream failure surfaces as TrtcUpstreamException

- **WHEN** `CreateCloudTranscription` fails with `TencentCloudSDKException`
- **THEN** the service throws `TrtcUpstreamException` and does not return success `StartTranscriptionData`
