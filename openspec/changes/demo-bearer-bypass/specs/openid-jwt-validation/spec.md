## ADDED Requirements

### Requirement: Optional demo Bearer exception on ConvoAI agents

在保持「`/api/**` 默认 JWKS JWT」的前提下，系统 MAY 提供可选演示旁路：当 `api.security.demo-token-enabled=true` 时，对 `POST`/`DELETE` `/api/convoai/agents`，若 Bearer 精确等于 `api.security.demo-token`，系统 MUST 视为已认证且 `sub`=`api.security.demo-user-id`，MUST NOT 要求该串通过 JWKS。该例外 MUST NOT 适用于其它受保护 `/api/**` 路径。开关关闭时行为 MUST 与原规格一致（仅合法 Users JWT）。

#### Scenario: Demo bypass limited to convoai agents

- **WHEN** 演示开关开启且仅 `/api/convoai/agents` 携带配置的固定 Bearer
- **THEN** 系统 MUST 允许该请求，MUST NOT 将其固定串视为其它 `/api/**` 的有效凭证
