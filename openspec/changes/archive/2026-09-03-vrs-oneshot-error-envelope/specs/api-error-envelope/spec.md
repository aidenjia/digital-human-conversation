> **已取代（勿作现行契约）**：本 delta 仍以一句话复刻 **4201** 等与 HTTP 分离为例。现行主规格中 VRS 失败已改为 `code` 与 HTTP 对齐；独立业务码分离仍适用于鉴权 4010/4030（commit `304ad6b`）。本文件仅历史快照。

## MODIFIED Requirements

### Requirement: HTTP status and ApiResponse code alignment

对于已处理的 `/api/**` 错误，系统 SHALL 返回统一 `ApiResponse` 信封。默认情况下，常见错误（如校验 400、上游 502、配置 503）的 HTTP 状态码 SHOULD 与 `ApiResponse.code` 相同。

当能力显式定义**独立业务码**（例如定制音色一句话复刻的 4201–4204，或鉴权的 4010 / 4030）时，系统 MUST 允许 `ApiResponse.code` 与 HTTP 状态码分离：HTTP 使用标准状态（如 429 / 400 / 502 / 401 / 403），业务语义以响应体 `code` 为准。系统 MUST NOT 将诸如 4201 的业务码直接用作 HTTP 状态码。

#### Scenario: Validation error keeps aligned codes

- **WHEN** 请求体校验失败且无独立业务码
- **THEN** HTTP status is 400 and `code` is 400

#### Scenario: Dedicated business code separates from HTTP

> 【已废止】下列「配额 → body code 4201」场景已取消；现行配额为 HTTP/`code` **429**。本场景仅保留作历史；鉴权 4010/4030 分离仍有效。

- **WHEN** 一句话复刻配额耗尽并映射为业务码 4201【已废止】
- **THEN** HTTP status is 429 and `ApiResponse.code` is 4201【已废止】

#### Scenario: Auth invalid token keeps separated codes

- **WHEN** Bearer token 无效
- **THEN** HTTP status is 401 and `code` is 4010
