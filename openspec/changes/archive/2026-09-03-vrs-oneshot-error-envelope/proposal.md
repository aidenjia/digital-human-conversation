> **已取代（勿作现行契约）**：本文仍写独立业务码 **4201～4204**。现行已改为 `code` 与 HTTP 对齐 + 友好 `message`（见主规格与 commit `304ad6b`）。本文件仅历史快照。

## Why

定制音色一句话复刻失败时，腾讯 VRS 错误被糊成笼统 400/502，且常夹带上游原文，前端无法稳定区分配额耗尽、音质不合格与参数错误。防连点 409 已有信封，但 VRS 侧缺少独立业务码。本期将 VRS 失败映射为稳定 `ApiResponse.code`（4201–4204）与英文友好文案，便于客户端分支。

## What Changes

**VRS 错误映射**
- From: SDK/`DetectionCode` 失败 → 多为 `UpstreamException` 502 或笼统 `voice detect failed` / `create training task failed`
- To（【已废止】独立业务码）：~~映射为业务码 4201（配额/限流）、4202（音质/检测）、4203（参数/音频）、4204（其它上游）；HTTP 分别为 429/400/400/502~~ → **现行**：HTTP/`code` 分别为 429/400/400/502（无 420x）；英文固定 `message`；可选 `error.reason` 带腾讯官方 Code
- Reason: ~~前端可认独立 code~~【已废止】→ 前端展示友好 `message`，粗分类认 HTTP/`code`
- Impact: 破坏性——依赖旧英文短句、「一律 502」或曾约定的 **420x** 的客户端均需按现行口径改

**ApiException HTTP 分离**
- From: `code` 与 HTTP status 恒等
- To: 支持 `httpStatus` 与 `code` 分离（默认仍相等以兼容）；Handler 用 `httpStatus`，信封用 `code`
- Reason: 4201 不能作为 HTTP 状态码
- Impact: 非破坏性（旧调用不变）

**lock4j 409**
- From/To: 保持现有 409 + 英文防连点文案
- Reason: 语义已正确；本期仅在契约中与 420x 并列说明
- Impact: 无行为变更

## Capabilities

### New Capabilities

- （无）

### Modified Capabilities

- `custom-voice-production`: 一句话复刻相关 API 失败时的业务码 / HTTP / 信封映射
- `api-error-envelope`: `ApiException` 允许业务码与 HTTP 分离（若该 spec 有相关要求则补 delta；否则以 custom-voice 为主、README 说明）

## Impact

- 代码：`ApiException`、`GlobalExceptionHandler`、`RealTencentVrsClient`（及 mapper）、`CustomVoiceProductionService` 透传
- API：`/api/custom-voices/**` 失败码变化；lock4j 409 不变
- 依赖/CD：无新中间件
- 文档：README 定制音色错误表；OpenSpec `custom-voice-production`
- Non-goals：i18n、声网 422、TTS/COS 专项、轮询 `failReason` 双语归一、全站错误码重构
