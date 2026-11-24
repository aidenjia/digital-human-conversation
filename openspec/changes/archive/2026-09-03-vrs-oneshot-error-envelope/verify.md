# Verify Receipt

**Change**: `vrs-oneshot-error-envelope`
**Verified at**: `2026-09-03`

> **已取代（勿作现行契约）**：本收据验证的是当时的 **420x** 映射。现行已改为 `code` 与 HTTP 对齐 + 友好 `message`（commit `304ad6b`）。本文件仅历史快照。

## Checks

> 【已废止】下列 Apifox 项中的 **4202/4203** 为当时验证口径；现行对应 HTTP/`code` **400**（看 `message` 区分）。

| Check | Result |
|-------|--------|
| Tasks complete | Yes (10/10) |
| Unit tests (`VrsExceptionMapperTest` / `CustomVoiceProductionServiceTest` / lock4j 409) | Pass |
| Local Apifox：~~4202~~【已废止】Detect 拒绝（现 code=400） | Pass（当时） |
| Local Apifox：~~4203~~【已废止】时长超限（现 code=400） | Pass（当时） |
| Develop Apifox：~~4203~~【已废止】时长超限（信封已部署） | Pass（当时） |
| 409 防连点 | 此前已测通 |
| Spec delta 与实现一致 | Yes（相对当时 420x；其后主规格已改为方案 A） |

## Gaps / accepted risks

- ~~4201 / 4204~~【已废止】真机难造 → 现为 429 / 502，仍可依赖单测映射覆盖
- `verify`/`finalize` 在归档前补写收据；PR 合入由有权限同事完成
