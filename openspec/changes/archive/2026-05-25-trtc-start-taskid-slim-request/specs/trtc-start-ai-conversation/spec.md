## MODIFIED Requirements

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
