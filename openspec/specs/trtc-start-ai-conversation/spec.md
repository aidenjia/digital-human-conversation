# trtc-start-ai-conversation Specification

## Purpose

TBD — update after consolidating archived OpenSpec changes for TRTC Start AI Conversation.
## Requirements
### Requirement: Start AI conversation HTTP endpoint

The system SHALL expose `POST /api/v1/trtc/ai-conversations/start` (or an equivalent path documented in the same change) under `con.aidenjia.digitalhuman.conversation.api`, accepting a JSON body that MAY include an optional `sessionId` for idempotency and SHALL NOT require `roomId` or `roomIdType` from the client, and SHALL invoke Tencent Cloud TRTC **StartAIConversation** when configuration and authentication succeed.

#### Scenario: Successful start returns join credentials and task id in envelope

- **WHEN** a client sends a valid authenticated request and server-side TRTC/LLM configuration is complete
- **THEN** the response is HTTP 200 JSON with shape `ResponseBody<StartConversationData>` where `code` is `0`, `message` indicates success, and `data` contains:
  - `sdkAppId` (number): TRTC application SdkAppId used for client UserSig
  - `userId` (string): client TRTC user id parsed from the authentication header
  - `userSig` (string): UserSig for that client user id
  - `strRoomId` (string): server-assigned string room id (from optional `sessionId` or a generated id)
  - `taskId` (string): Tencent `TaskId` from StartAIConversation
- **AND** the Tencent request uses `roomIdType` string room (`1`) with the server-assigned `strRoomId` as `RoomId`

#### Scenario: Invalid body returns HTTP 400

- **WHEN** a client sends a request body that fails Jakarta Bean Validation for the start endpoint DTO
- **THEN** the response status is 400 and the body follows the existing Problem Details pattern used by the application

### Requirement: Header authentication for TRTC start routes only

The system SHALL apply fixed-token authentication only to paths under `/api/v1/trtc/**` (or the concrete start path prefix used by the implementation) and SHALL NOT require this token for other existing business routes such as `POST /api/v1/echo`.

#### Scenario: Missing or invalid token on TRTC route

- **WHEN** a client calls the start endpoint without the configured header or with a token that fails validation
- **THEN** the response status is 401 and no TRTC cloud API call is made

#### Scenario: Echo without TRTC token

- **WHEN** a client calls `POST /api/v1/echo` with a valid echo body and without the TRTC API token header
- **THEN** the response remains HTTP 200 as today (assuming no other auth applies)

### Requirement: Client TRTC UserId parsed from the authentication header value

The system SHALL derive the TRTC **TargetUserId** (client user id for the robot to subscribe/process) by parsing the configured authentication header value using the format **`<userId>|<secret>`** where the first unescaped segment before the first `|` is the user id and the remainder after the first `|` is the secret; the secret MUST match the configured fixed API token from environment or configuration. The user id segment MUST NOT be empty and MUST NOT contain `|`.

#### Scenario: Valid composite token sets target user

- **WHEN** the header value is `alice|correct-secret` and the configured fixed token is `correct-secret`
- **THEN** `alice` is used as `AgentConfig.TargetUserId` for the StartAIConversation request

#### Scenario: Wrong secret is rejected

- **WHEN** the header value is `alice|wrong-secret`
- **THEN** the response status is 401

### Requirement: Robot UserId is deployment-scoped and globally unique

The system SHALL set `AgentConfig.UserId` to a non-empty robot user id provided only via configuration or environment (e.g. `TRTC_ROBOT_USER_ID`) and SHALL NOT accept robot user id from the client request body for this endpoint.

#### Scenario: Robot id comes from configuration

- **WHEN** the start endpoint is invoked with valid authentication and body
- **THEN** the Tencent request uses the configured robot `UserId` regardless of any client-supplied field

### Requirement: Fixed Tencent API region ap-guangzhou

The system SHALL call Tencent TRTC cloud API with region **`ap-guangzhou`** for StartAIConversation in this change (no multi-region selection from the client).

#### Scenario: Region is not client-driven

- **WHEN** any client calls the start endpoint
- **THEN** the Tencent client uses `ap-guangzhou` as the configured region

### Requirement: LLMConfig is fixed from files and secrets from environment

The system SHALL build the `LLMConfig` JSON string from a classpath JSON template (or equivalent configuration file) bundled with the application and SHALL merge sensitive fields such as `APIKey` from environment or secured configuration. The system SHALL NOT accept a full LLM configuration object from the client request body in this change.

#### Scenario: API key is merged at runtime

- **WHEN** the LLM template file omits or placeholders the API key and `TRTC_LLM_API_KEY` (or bound property) is set
- **THEN** the outbound Tencent request contains a valid `LLMConfig` string including the merged key

### Requirement: TTSConfig is server-fixed

The system SHALL supply `TTSConfig` as a fixed JSON string loaded from server-side configuration or classpath resource and SHALL NOT read TTS parameters from the client request body for this change.

#### Scenario: Client cannot override TTS

- **WHEN** a client sends extra TTS-related fields in the JSON body
- **THEN** those fields are ignored for Tencent `TTSConfig` construction

### Requirement: No logging of full LLMConfig

The system MUST NOT write the full `LLMConfig` JSON string, API keys, or `UserSig` to application logs at INFO or DEBUG levels for this feature. Structured logs MAY include non-sensitive correlators (`TaskId`, `RequestId`, `RoomId`, `SdkAppId`, robot and target user ids).

#### Scenario: Debug logging stays safe

- **WHEN** application log level for the service package is DEBUG and a conversation is started successfully
- **THEN** no log line contains the complete `LLMConfig` payload as emitted to Tencent

### Requirement: Tencent Java SDK uses pinned Maven versions

The system SHALL declare `com.tencentcloudapi:tencentcloud-sdk-java-trtc` with an explicit version aligned to Maven Central (and compatible `tencentcloud-sdk-java-common` via transitive resolution or explicit same-version pin) and SHALL NOT use `LATEST` or unversioned floating coordinates for these artifacts in `pom.xml`.

#### Scenario: Reproducible dependency tree

- **WHEN** a developer runs `mvn dependency:tree` for the project
- **THEN** Tencent TRTC SDK artifacts resolve to fixed numeric versions without `LATEST`

### Requirement: Secrets and sensitive TRTC settings use environment

The system SHALL read Tencent Cloud API credentials (`TRTC_CLOUD_SECRET_ID`, `TRTC_CLOUD_SECRET_KEY`), TRTC application SdkAppId for API calls (`TRTC_SDK_APP_ID`), LLM API keys, and the fixed API authentication token from environment variables or secure externalized configuration suitable for production; the repository SHALL NOT require committing plaintext secrets for the feature to be described.

Robot **UserSig** (TLS signature) SHALL use **separate** configuration from CAM cloud API keys: `TRTC_USERSIG_SDK_APP_ID` and `TRTC_USERSIG_SECRET_KEY` (TRTC console application key material), bound as `trtc.usersig.sdk-app-id` and `trtc.usersig.secret-key`, because the TRTC console key pair can differ from `TRTC_SDK_APP_ID` / `TRTC_CLOUD_SECRET_KEY`.

`StartAIConversationRequest` SHALL continue to use `trtc.sdk-app-id` / `TRTC_SDK_APP_ID` for the request’s application identifier where the API requires it.

UserSig expiry is fixed in committed defaults via `trtc.usersig.expire-seconds` (not required in `.env` templates).

#### Scenario: Properties use env placeholders

- **WHEN** inspecting committed default properties for this feature
- **THEN** sensitive values are referenced via `${ENV_NAME:}` style placeholders or are empty defaults with documentation pointing operators to set environment variables

#### Scenario: UserSig env vars documented

- **WHEN** inspecting committed `.env` templates and operator documentation for this feature
- **THEN** `TRTC_USERSIG_SDK_APP_ID` and `TRTC_USERSIG_SECRET_KEY` are listed for UserSig generation, distinct from `TRTC_CLOUD_SECRET_KEY`

#### Scenario: UserSig uses dedicated SdkAppId and secret key

- **WHEN** the start endpoint generates a robot UserSig
- **THEN** `TLSSigAPIv2` uses `trtc.usersig.sdk-app-id` and `trtc.usersig.secret-key` (from `TRTC_USERSIG_*`), not `TRTC_CLOUD_SECRET_KEY`

### Requirement: Stop and control conversation APIs are out of scope

The system SHALL NOT add StopAIConversation, control, or lifecycle management endpoints as part of this change.

#### Scenario: No stop route in this change

- **WHEN** the change is implemented and archived
- **THEN** no new `/api/v1/trtc/**` route is documented or implemented solely for stopping or controlling an AI conversation task within this change scope

