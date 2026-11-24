> **已取代（勿作现行契约）**：本 delta 仍要求 **4201～4204**。现行主规格已改为 `code` 与 HTTP 对齐 + 友好 `message`（commit `304ad6b`）。本文件仅历史快照。

## ADDED Requirements

### Requirement: One-shot VRS failures use dedicated business codes

当腾讯一句话复刻（VRS）调用失败时，系统 MUST 将错误映射为统一 `ApiResponse` 信封，且响应体业务 `code` MUST 为下列之一（英文友好 `message`；MAY 在 `error.reason` 中携带腾讯官方 Error.Code 或 `VRS_DETECT_REJECTED`）。HTTP 状态 MUST 按下表，且 MUST NOT 要求 HTTP 数值等于业务 `code`（420x 场景）。

> 【已废止】下表 **4201～4204** 已取消，勿再对接；现行为 `code`=HTTP + 友好 `message`（见主规格 / `304ad6b`）。

| 业务 code【已废止】 | 含义 | HTTP |
|-----------|------|------|
| 4201【已废止】 | 配额耗尽或请求限流 | 429 |
| 4202【已废止】 | 音质/环境检测未通过（含 Detect `DetectionCode≠0`） | 400 |
| 4203【已废止】 | 音频或参数不合规 | 400 |
| 4204【已废止】 | 其它 VRS 上游失败 | 502 |

系统 MUST 至少将 `UnsupportedOperation.VRSQuotaExhausted`、以及 `LimitExceeded` / `RequestLimitExceeded`（含子码前缀）映射为 4201；将 `FailedOperation.VoiceNotQualified`、`FailedOperation.VoiceEvaluateFailed` 与 Detect 业务拒绝映射为 4202；将 `InvalidParameter` / `InvalidParameterValue`（含子码前缀）映射为 4203；其余已识别为 VRS SDK/上游失败且未命中上列者映射为 4204。系统 MUST NOT 将腾讯错误原文作为唯一对外 `message`。`CustomVoiceProductionService`（及同类编排）在捕获已映射的此类失败时 MUST 原样抛出对应 `ApiException`，MUST NOT 再包装为笼统的 `"voice detect failed"` / `"create training task failed"` 且丢失 420x `code`。

#### Scenario: Quota exhausted maps to 4201【已废止→现 429】

- **WHEN** CreateVRSTask（或其它 VRS 调用）返回 `UnsupportedOperation.VRSQuotaExhausted`
- **THEN** 系统 MUST 返回 HTTP 429，且 `ApiResponse.code` MUST 为 ~~4201~~【已废止】**429**，`success` MUST 为 false

#### Scenario: Detect quality rejection maps to 4202【已废止→现 400】

- **WHEN** Detect 返回 `DetectionCode≠0`，或 SDK 返回 `FailedOperation.VoiceNotQualified`
- **THEN** 系统 MUST 返回 HTTP 400，且 `ApiResponse.code` MUST 为 ~~4202~~【已废止】**400**

#### Scenario: Invalid audio params map to 4203【已废止→现 400】

- **WHEN** VRS 返回 `InvalidParameterValue.AudioDurationExceedsLimit`（或同类 InvalidParameter*）
- **THEN** 系统 MUST 返回 HTTP 400，且 `ApiResponse.code` MUST 为 ~~4203~~【已废止】**400**

#### Scenario: Unknown VRS failure maps to 4204【已废止→现 502】

- **WHEN** VRS SDK 失败且 Error.Code 未命中配额/音质/参数映射
- **THEN** 系统 MUST 返回 HTTP 502，且 `ApiResponse.code` MUST 为 ~~4204~~【已废止】**502**，`message` MUST 为英文友好文案而非上游全文

#### Scenario: Mapped ApiException is not re-wrapped

- **WHEN** submit 流程捕获已映射的 VRS `ApiException`（原文：业务码 4202【已废止】）
- **THEN** 系统 MUST 向客户端保留原 HTTP/`code`/`message`，MUST NOT 改为笼统 400 `"voice detect failed"`

---

### Requirement: Custom voice lock failure remains 409

定制音色提交等带 `@Lock4j` 的接口在抢锁失败时，系统 MUST 继续返回 HTTP 409 与 `ApiResponse.code` 409，以及英文进行中提示（与 join 防连点相同语义）。系统 MUST NOT 将 lock4j 409 映射为 420x。

#### Scenario: Concurrent custom voice submit returns 409

- **WHEN** 同一用户定制音色提交进行中且第二请求抢锁失败
- **THEN** 系统 MUST 返回 HTTP 409 且 `code` 为 409
