## Why

演示环境难以每次申请 Users OpenID access token，阻塞 ConvoAI join/leave 联调。需要在可开关前提下用固定 Bearer 快速进房，同时 UE5 仍复用库内用户配置，避免改业务编排。

## What Changes

**ConvoAI agents 鉴权旁路**
- From: `POST|DELETE /api/convoai/agents` 仅接受 Users JWT（JWKS + iss + exp）
- To: 当 `api.security.demo-token-enabled=true` 且 Bearer 精确等于配置 `demo-token` 时，跳过 JWKS，合成 `sub=demo-user-id`；否则仍走原 JWT
- Reason: 快速演示
- Impact: 非破坏（双轨）；演示客户端换 token；其它 `/api/**` 不变

**配置**
- 新增 `demo-token-enabled` / `demo-token` / `demo-user-id`；dev 明文写入；prod 默认关闭

**数据（运维）**
- 演示库预置 `user_companion_config`：`user_id=demo-user-id`，`voice_source=preset`

## Capabilities

### New Capabilities
- `convoai-demo-bearer`: 可选固定 Bearer 旁路（仅 convoai agents）与配置/种子约定

### Modified Capabilities
- `openid-jwt-validation`: 允许在开关开启时对 `/api/convoai/agents` 接受配置固定 Bearer 作为认证例外

## Impact

- `ApiSecurityProperties`、`ResourceServerSecurityConfig`、新 Filter/Resolver
- `application.yml` / `application-dev.yml` / `application-prod.yml`
- README 演示说明；MockMvc 鉴权测试
- 无新中间件依赖；ConvoaiService / Companion 不改
