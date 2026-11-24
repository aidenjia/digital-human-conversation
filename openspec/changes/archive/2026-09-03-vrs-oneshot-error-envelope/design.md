> **已取代（勿作现行契约）**：本文仍写独立业务码 **4201～4204**。现行已改为 `code` 与 HTTP 对齐 + 友好 `message`（见主规格与 commit `304ad6b`）。本文件仅历史快照。

## Context

- 部署：`replicas: 2`（prod base）；dev overlay 可为 1；无粘滞。中间件：Postgres + Redis + 腾讯 COS/VRS/TTS。
- 定制音色一句话复刻：`GetTrainingText` → corpus COS → `DetectEnvAndSoundQuality` → `CreateVRSTask` → 轮询 `DescribeVRSTaskStatus`。
- 现状：`RealTencentVrsClient.wrapSdk` → 一律 `UpstreamException`(502)；Detect `DetectionCode≠0` 亦 Upstream；submit 再压成笼统 400/502。
- 鉴权已有先例：响应体 `code` 4010/4030 与 HTTP 401/403 分离（`AuthFailureResponses`）。
- lock4j 抢锁失败已是 HTTP/code 409 + 英文 message。
- 腾讯官方业务码见 [声音复刻错误码](https://cloud.tencent.com/document/product/1283/90074)（如 `UnsupportedOperation.VRSQuotaExhausted`）。

## Goals / Non-Goals

**Goals:**

- VRS 一句话复刻失败 → ~~稳定业务码 4201–4204~~【已废止】→ HTTP/`code` 对齐 + 英文友好 message + 统一信封
- HTTP 使用标准 429/400/502；~~与业务码可分离~~【已废止】→ 与 `code` 相同（鉴权 4010/4030 仍可分离）
- 已映射的 `ApiException` 在 Service 层原样抛出
- 409 防连点语义与文案保持
- 单测覆盖主要映射分支；README 写清对外表

**Non-Goals:**

- i18n（`X-Api-Culture` 切换中英文案）
- 声网 ConvoAI 422/409、TTS/COS 专项映射
- 轮询态 `failReason` 全面归一（可二期）
- 自造全站 4xxx 错误码体系改造

## Decisions

1. **业务码表**（【已废止】4201～4204；409 仍有效）：~~4201 配额/限流；4202 音质/检测；4203 参数/音频；4204 其它 VRS~~ → 现行改为 HTTP/`code` 429/400/502 + 友好 message；409 lock4j。
2. **ApiException**：增加 `httpStatus`（默认=`code`）、可选 `error`；`GlobalExceptionHandler` 使用 `getHttpStatus()` 与 `fail`/`failWithError`。
3. **映射位置**：`infrastructure.vrs` 内集中 mapper（读 `TencentCloudSDKException.getErrorCode()`）；Detect 业务拒绝 → ~~4202~~【已废止】现 HTTP/code 400 + reason `VRS_DETECT_REJECTED`。
4. **配额识别**：`UnsupportedOperation.VRSQuotaExhausted`、`LimitExceeded`、`RequestLimitExceeded` 前缀 → ~~4201~~【已废止】现 HTTP/`code` 429。
5. **参数类**：`InvalidParameter` / `InvalidParameterValue` 前缀 → ~~4203~~【已废止】现 HTTP/`code` 400。
6. **透传**：`CustomVoiceProductionService` catch 时若已是映射后的 VRS `ApiException` 则 rethrow（仍可 markFailed）；勿再包成 `"voice detect failed"`。（原文曾写「带 420x」，【已废止】）
7. **文案**：固定英文常量；不引入 MessageSource。
8. **可选 reason**：`error: { "reason": "<官方Code或VRS_DETECT_REJECTED>" }`；**【已废止】勿再以 420x 为前端分支**，现行展示友好 `message`。

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| 客户端依赖旧 message/502 | README + 破坏性说明；~~认 420x~~【已废止】→ 认 HTTP/`code` + `message` |
| 未知腾讯 Code 落入 ~~4204~~【已废止】 | 现落入 502；日志打官方 Code；可后续加映射 |
| Handler 误用 `code` 作 HTTP | ~~单测断言 HTTP 429 + body code 4201~~【已废止】→ 现断言二者均为 429 |
| markFailed 仍写英文短语 | 与 message 对齐即可；非对外契约重点 |

## Migration Plan

1. 发版后定制音色失败返回 420x；前端改分支
2. 回滚：恢复笼统 400/502（不推荐）
3. 无 DB/CD 迁移

## Open Questions

无。
