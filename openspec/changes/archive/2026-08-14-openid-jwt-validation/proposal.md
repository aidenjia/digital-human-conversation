## Why

客户端使用 OpenID Connect provider OpenID access token。conversation 须在本地校验 JWT，不能再依赖固定 Bearer，也不能对 Users 做 introspection。

## What Changes

**鉴权模型**
- From: 过渡固定 token（已删除）
- To: OAuth2 Resource Server，JWKS 验签 + `iss` + `exp`（默认 clock skew 60s）
- Reason: 与 Users 发行的 `at+jwt` 对齐；无双轨
- Impact: 客户端必须带 Users access token

**受保护范围**
- From: brainstorm 仅 `/api/convoai/**`
- To: `/api/convoai/**`、陪伴与次元配置/目录 API 一律 `authenticated()`；`GET /`、`/api/echo`、actuator health/prometheus、`OPTIONS` 公开
- Reason: 后续产品 API 与 join 共用同一套 JWT
- Impact: 配置页接口同样 401/4010

## Capabilities

### New Capabilities

- `openid-jwt-validation`: Users JWT 本地校验、信封错误码、受保护路径、`sub` 作为 user_id

### Modified Capabilities

- 无（不改 `api-error-envelope` 字段形状；仅使用已有 `code`/`message`）

## Impact

- 代码：`ResourceServerSecurityConfig`、`ApiSecurityProperties`、`AuthFailureResponses`、`CurrentUserIdAccessor`
- 配置：`API_SECURITY_ISSUER_URI` / `JWK_SET_URI` / `ADDITIONAL_ISSUER_URIS`
- 测试：`JwtResourceServerAuthTest`、各 API Auth 切片
- 不做：role、introspection、fixed token
