## MODIFIED Requirements

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
