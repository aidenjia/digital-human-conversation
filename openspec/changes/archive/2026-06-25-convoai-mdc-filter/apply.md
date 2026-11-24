# Apply Receipt

> Generated at the end of the apply phase to mark code-implementation
> complete and provide verify with the state it needs.
> Overwritten on each apply iteration; iteration counter grows.

**Change**: `convoai-mdc-filter`
**Iteration**: `1`
**Applied at**: `2026-06-25 12:04`
**Executor**: `subagent-driven-development`

---

## Workspace

- **Worktree**: `<local-path>`（主工作区）
- **Branch**: `feature/superSpec`

---

## Commits

- **Range**: `none`（变更尚未提交）
- **Count**: `0`

---

## Tasks

- **Completed**: `10 of 10` checkboxes in tasks.md flipped to `- [x]`
- **Remaining**: `none`

---

## Implementation notes

- 初版使用 `ContentCachingRequestWrapper` 导致 `@RequestBody` 无法重复读 body（`ConvoaiControllerTest` 400 Malformed JSON）；已改为 `CachedBodyHttpServletRequest`。
- `ConvoaiService.leave` 不写入 MDC（由 Filter `bindLeave` 负责）；`ConvoaiServiceTest` 增加 `@AfterEach ConvoaiLogContext.clear()` 防止线程泄漏。

## Files touched (this change)

| Area | Path |
|------|------|
| MDC 上下文 | `trtc/support/ConvoaiLogContext.java` |
| Filter | `security/ConvoaiMdcFilter.java`, `security/CachedBodyHttpServletRequest.java` |
| 注册 | `config/WebSecurityConfig.java` |
| Service | `trtc/ConvoaiService.java` |
| 测试 | `ConvoaiLogContextTest`, `ConvoaiMdcFilterTest`, `ConvoaiServiceTest` |
| 规范 | `.cursor/rules/application-logging.mdc` |

## Verification

- `mvn verify`：**通过**（2026-06-25）

---

## Next step

Run `/opsx:verify`，通过后 `/opsx:archive` 将 delta 合并至 `openspec/specs/convoai-request-logging/`。
