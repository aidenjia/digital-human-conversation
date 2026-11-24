## 1. Configuration & properties

- [x] 1.1 Add `BailianRealtimeProperties` (`bailian.realtime.*`: api-key, workspace-id, model, default-voice, default-instructions, token-ttl-seconds) bound from env
- [x] 1.2 Wire defaults in `application.yml`; local/dev placeholders via env only (no secrets in Git)
- [x] 1.3 Register properties in existing configuration enablement pattern

## 2. RTC credentials API

- [x] 2.1 Add `api.rtc.dto` response record and optional empty request
- [x] 2.2 Add thin `RtcCredentialsController` → service → `RoomIdGenerator` + `AgoraRtcTokenService.buildUserToken`
- [x] 2.3 Secure path under existing `/api/**` JWT resource server rules
- [x] 2.4 MockMvc: missing/invalid bearer reject; valid bearer returns appId/channel/userId/token without agentId

## 3. Bailian temporary token API

- [x] 3.1 Add `infrastructure.bailian` OkHttp client for `POST /api/v1/tokens?expire_in_seconds=`
- [x] 3.2 Add service: clamp TTL, call client, build `wsUrl`; fixed instructions/voice from yml（本期不做欢迎语/人设解析）
- [x] 3.3 Add `api.bailian` Controller + DTOs; map ConfigurationException / UpstreamException to envelope
- [x] 3.4 Ensure logging never prints permanent or temporary keys
- [x] 3.5 MockMvc/unit: auth failures; missing config; upstream failure; success shape; UE5 companion instructions; fallback defaults; no ConvoAI join side effects

## 4. Docs & verify

- [x] 4.1 Update `README.md` (and client integration doc if present) with dual endpoints, client WS/3A checklist, Non-goals
- [x] 4.2 Run `mvn spotless:apply` and `mvn verify`
