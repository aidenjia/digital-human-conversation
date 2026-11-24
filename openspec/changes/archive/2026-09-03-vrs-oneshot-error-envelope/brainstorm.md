> **已取代（勿作现行契约）**：本文仍写独立业务码 **4201～4204**。现行已改为 `code` 与 HTTP 对齐 + 友好 `message`（见主规格与 commit `304ad6b`）。本文件仅历史快照。

## Design Summary

定制音色一句话复刻（腾讯 VRS）失败时，本服务将上游错误映射为**独立业务码** + 统一 `ApiResponse` 信封（英文友好 `message`），HTTP 状态与业务码可分离。本服务 lock4j **409** 保持现有防连点语义。不做 i18n（产品文案统一英文）。非目标：声网 422、TTS/COS 欠费、全站错误码改造。

## Alternatives Considered

### Alternative A: HTTP code + English message only
- **Approach**: 配额→429、音质→400、其它→502；靠 `message` 区分
- **Pros**: 改动最小；与现行 `ApiException(code==HTTP)` 一致
- **Cons**: 全站大量 400/502，前端无法仅靠数字分支；文案一改即脆
- **Why not chosen**: 用户明确要求独立业务码更清楚

### Alternative B: HTTP 档位 + `error.reason`（腾讯官方 Code）
- **Approach**: `code` 仍用 400/429/502；`error.reason` 带官方字符串
- **Pros**: 细分能力强；HTTP 标准
- **Cons**: 前端必须认 `reason`；数字 400 仍与其它接口冲突
- **Why not chosen**: 用户更倾向独立 `code` 数字

### Alternative C: 独立业务码 420x + HTTP 分离（选定）
- **Approach**: 响应体 `code`=4201–4204（及既有 409）；HTTP 用 429/400/502/409；可选 `error.reason` 带腾讯 Code
- **Pros**: 前端认业务码即可；不与鉴权 4010/4030、普通 400 校验撞语义；HTTP 仍标准
- **Cons**: 需扩展 `ApiException` / Handler（`code` ≠ HTTP）；比 A 多一点改动
- **Why not chosen**: N/A — 选定

## Agreed Approach

采用 **Alternative C**：

> 【已废止】下表中 **4201～4204** 行已取消，勿再对接；现行为 `code`=HTTP + 友好 `message`（见主规格 / `304ad6b`）。**409** 防连点仍有效。

| 业务 `code` | 含义 | 典型腾讯来源 | HTTP | 状态 |
|-------------|------|--------------|------|------|
| 409 | 防连点 | lock4j | 409 | 仍有效 |
| 4201【已废止】 | 配额/限流 | `UnsupportedOperation.VRSQuotaExhausted`、`LimitExceeded`、`RequestLimitExceeded*` | 429 | 已废止→现用 429 |
| 4202【已废止】 | 音质/检测未过 | `VoiceNotQualified`、`VoiceEvaluateFailed`、Detect `DetectionCode≠0` | 400 | 已废止→现用 400 |
| 4203【已废止】 | 音频/参数不合规 | `InvalidParameterValue.*` 等 | 400 | 已废止→现用 400 |
| 4204【已废止】 | 其它 VRS 上游失败 | 未知 SDK / InternalError 等 | 502 | 已废止→现用 502 |

英文 `message` 固定友好句；可选 `error.reason` = 官方 Code（或 `VRS_DETECT_REJECTED`）。训练异步失败仍以状态接口 `failReason` 为主。

## Key Decisions

1. 文案：**全部英文**（不做本期 i18n）
2. 范围：一句话复刻 VRS 链路 + 本服务 409；不含声网/TTS/COS 专项映射
3. 前端以 ~~**`ApiResponse.code`（420x）**~~【已废止】→ **HTTP/`code` + 友好 `message`**；勿再按 420x 分支
4. 改 `ApiException`：增加 `httpStatus`（默认等于 `code` 以兼容旧调用）与可选 `error`
5. `CustomVoiceProductionService` 已映射的 `ApiException` 原样抛出，禁止再包成笼统 400/502

## Open Questions

- 无阻塞项。可选：轮询 `failReason` 是否二期归一为同一套短语（本期可不做）。
