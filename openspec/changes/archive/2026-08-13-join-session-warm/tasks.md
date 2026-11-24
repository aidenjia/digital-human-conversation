## 1. Config & client

- [x] 1.1 Add `convoai.session-warm` properties (`enabled`, `timeout-ms` default 5000, optional `url`) on `ConvoaiProperties` + `application.yml`
- [x] 1.2 Implement `AgentSessionWarmClient` in `infrastructure` (OkHttp, Bearer from llm api-key, URL resolve/derive, call timeout)
- [x] 1.3 Warm request DTO/map: `clientUserId`, `conversationId`, `deviceId`, `digitalHumanType`, optional `timezone`; parse agent `code` without logging secrets

## 2. Service orchestration

- [x] 2.1 In `ConvoaiService.join`, after successful `restClient.join`, synchronously call warm when enabled; catch all failures as WARN
- [x] 2.2 Ensure Agora join failure path never calls warm; return credentials unchanged (no new response fields)

## 3. Tests & docs

- [x] 3.1 Unit tests: warm called once after Agora success; warm failure/timeout still returns credentials; disabled skips warm
- [x] 3.2 Update `README.md` and `docs/convoai-client-integration.md` (server-side best-effort warm after join)
- [x] 3.3 `mvn spotless:apply` and `mvn verify`
