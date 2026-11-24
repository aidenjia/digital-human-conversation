## MODIFIED Requirements

### Requirement: Start AI conversation HTTP endpoint

The system SHALL expose `POST /api/v1/trtc/ai-conversations/start` (or an equivalent path documented in the same change) under `con.aidenjia.digitalhuman.conversation.api`, accepting a JSON body that includes at least the TRTC room identifier and optional room id type and session id, and SHALL invoke Tencent Cloud TRTC **StartAIConversation** when configuration and authentication succeed.

#### Scenario: Successful start returns client join credentials in envelope

- **WHEN** a client sends a valid authenticated request with a valid body and server-side TRTC/LLM configuration is complete
- **THEN** the response is HTTP 200 JSON with shape `ResponseBody<StartConversationData>` where `code` is `0`, `message` indicates success, and `data` contains:
  - `sdkAppId` (number): TRTC application SdkAppId used for client UserSig
  - `userId` (string): client TRTC user id parsed from the authentication header
  - `userSig` (string): UserSig for that client user id
  - `strRoomId` (string): the request `roomId` (max 64 bytes as provided by client)
- **AND** Tencent `TaskId` and `RequestId` are recorded in server logs but are not required fields in the HTTP response body

#### Scenario: Invalid body returns HTTP 400

- **WHEN** a client sends a request body that fails Jakarta Bean Validation for the start endpoint DTO
- **THEN** the response status is 400 and the body follows the existing Problem Details pattern used by the application
