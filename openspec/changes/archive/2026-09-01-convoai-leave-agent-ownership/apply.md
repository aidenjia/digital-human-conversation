# Apply Receipt

**Change**: `convoai-leave-agent-ownership`
**Iteration**: `1`
**Applied at**: `2026-09-01`
**Executor**: in-session on `feature/convoai-leave-agent-ownership`

---

## Workspace

- **Branch**: `feature/convoai-leave-agent-ownership`
- **Worktree**: none

---

## Tasks

- **Completed**: 8 of 8 in `tasks.md`

---

## Implementation summary

- Redisson `agentId → JWT sub` ownership store (TTL configurable)
- `ConvoaiService.join` binds after Agora success (best-effort)
- `ConvoaiService.leave` verifies ownership; deny HTTP 404; unbind on success
- Bean wiring via `RedissonConfig` + NoOp only when redis address absent
- Unit tests + README leave ownership docs

---

## Verification notes (apply-time)

- `mvn spotless:apply` / related unit tests passed
- Apifox: other-user leave → 404; owner leave → 200 STOPPED
