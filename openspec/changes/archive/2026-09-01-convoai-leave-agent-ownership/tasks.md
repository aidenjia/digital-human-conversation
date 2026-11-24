## 1. Ownership store

- [x] 1.1 Add `ConvoaiAgentOwnershipStore` interface (`bind` / `findOwner` / `unbind`) under `trtc.support`
- [x] 1.2 Implement Redis store (`convoai-agent:{agentId}` → userId, TTL from config) + NoOp + Spring `@Configuration` beans (mirror companion cache)
- [x] 1.3 Add optional TTL on `ConvoaiProperties` + `application.yml` default (1 day)

## 2. Service orchestration

- [x] 2.1 `ConvoaiService.join`: after Agora success, bind ownership (log on failure, do not throw)
- [x] 2.2 `ConvoaiService.leave`: require userId; deny 404 if missing/mismatch; Agora leave; unbind on success

## 3. Tests and docs

- [x] 3.1 Unit tests: owner leave OK; other user 404 (no Agora); unknown 404; join still OK when bind fails
- [x] 3.2 Update `README.md` leave section: ownership required; 404 for unauthorized/unknown
- [x] 3.3 `mvn spotless:apply` and `mvn verify`
