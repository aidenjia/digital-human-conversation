## ADDED Requirements

### Requirement: Optional demo Bearer for ConvoAI agents only

当且仅当 `api.security.demo-token-enabled=true` 时，系统 MUST 对 `POST /api/convoai/agents` 与 `DELETE /api/convoai/agents` 接受配置项 `api.security.demo-token` 的精确 Bearer 匹配作为认证成功路径，MUST 将业务 `user_id` 设为 `api.security.demo-user-id`（合成 JWT `sub`），且 MUST NOT 对该请求执行 Users JWKS 验签。系统 MUST 仍接受合法 Users JWT 访问上述路径（双轨）。当开关为 false 或未配置时，系统 MUST NOT 将任意固定串视为有效凭证。

对其它受保护 `/api/**` 路径，即使 Bearer 等于 `demo-token`，系统 MUST NOT 因此旁路 JWKS（须按原 JWT 规则失败或成功）。

启动时若 `demo-token-enabled=true`，系统 MUST 要求 `demo-token` 与 `demo-user-id` 均非空，否则 MUST 启动失败。

演示 UE5 配置 MUST 通过库表 `user_companion_config`（`user_id` = `demo-user-id`，`voice_source=preset`）提供；系统 MUST NOT 为演示另增 join 偏好代码分支。

鉴权失败信封 MUST 保持：缺/畸形 Authorization → HTTP 401 且 `code=401`；无效 token（含错误固定串且未旁路）→ HTTP 401 且 `code=4010`。日志 MUST NOT 输出 Bearer 原文。

#### Scenario: Demo token accepts join when enabled

- **WHEN** `demo-token-enabled=true` 且客户端对 `POST /api/convoai/agents` 携带 `Authorization: Bearer <demo-token>`
- **THEN** 系统 MUST 认证成功且业务 `user_id` 等于 `demo-user-id`

#### Scenario: Demo token rejected on other API

- **WHEN** `demo-token-enabled=true` 且客户端对非 `/api/convoai/agents` 的受保护 `/api/**` 仅携带 demo-token
- **THEN** 系统 MUST 拒绝（HTTP 401 且 `code=4010` 或项目等价无效 token）

#### Scenario: Disabled ignores demo token

- **WHEN** `demo-token-enabled=false` 且客户端对 join 携带 demo-token
- **THEN** 系统 MUST 按无效 JWT 拒绝（`code=4010`）

#### Scenario: Real JWT still works on agents

- **WHEN** `demo-token-enabled=true` 且客户端对 join 携带合法 Users JWT
- **THEN** 系统 MUST 认证成功且 `user_id` 为该 JWT 的 `sub`
