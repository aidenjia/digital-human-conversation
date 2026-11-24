# Finalize Receipt

**Change**: `vrs-oneshot-error-envelope`
**Finalized at**: `2026-09-03`

> **已取代（勿作现行契约）**：本归档仍描述当时的独立业务码 **4201～4204**。之后已改为方案 A——`ApiResponse.code` 与 HTTP 对齐（429/400/502/409），仅包装英文友好 `message`，前端直接展示即可。现行以主规格 `openspec/specs/api-error-envelope/spec.md`、`openspec/specs/custom-voice-production/spec.md` 及 commit `304ad6b` 为准。本目录仅作历史快照。

- Sync delta → `openspec/specs/api-error-envelope/spec.md`、`openspec/specs/custom-voice-production/spec.md`
- Archive to `openspec/changes/archive/2026-09-03-vrs-oneshot-error-envelope/`
- 实现曾在分支 `feature/vrs-oneshot-error-envelope`（commit `4e4edcb` 落地 420x；`304ad6b` 取消 420x 改对齐 HTTP）；合入 develop 由有权限同事 merge
