## MODIFIED Requirements

### Requirement: Start AI conversation HTTP endpoint

The system SHALL expose `POST /api/v1/trtc/ai-conversations/start` under `con.aidenjia.digitalhuman.conversation.api` with **no request body parameters**, SHALL derive the client TRTC user id from the configured authentication header only, and SHALL invoke Tencent Cloud TRTC **StartAIConversation** when configuration and authentication succeed.

#### Scenario: Successful start with server-generated session and room

- **WHEN** a client sends a valid authenticated request without a JSON body (or with an empty body that is ignored)
- **THEN** the server generates a unique `sessionId` (32-character hexadecimal, no hyphen separators)
- **AND** uses that value as both Tencent `SessionId` and string `RoomId` (`roomIdType` string room)
- **AND** the HTTP 200 response `data.strRoomId` equals the generated id
- **AND** `data` includes `sdkAppId`, `userId`, `userSig`, `strRoomId`, and `taskId` as documented

#### Scenario: Invalid body returns HTTP 400

- **WHEN** a client sends a request body that fails Jakarta Bean Validation for the start endpoint DTO
- **THEN** the response status is 400 and the body follows the existing Problem Details pattern used by the application
