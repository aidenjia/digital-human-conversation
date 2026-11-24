# Design: OpenID JWT Resource Server

## Context

代码已落地：`ApiAuthFilter` / 固定 Bearer 已删除；`ResourceServerSecurityConfig` + `NimbusJwtDecoder` 本地验签。集群 2 副本、JWKS 进程内缓存、无 Redis。

## 与 brainstorm 的漂移（以代码为准）

| 维度 | Brainstorm | 现行代码 |
|------|------------|----------|
| 受保护路径 | 仅 `/api/convoai/**` | 另含 companions / companion-voices / user-companion-configs / dimension-voices / user-dimension-configs |
| Token typ | 未写 | Users `typ=at+jwt`，`NimbusJwtDecoder.validateType(false)` |
| 4030 | 不做 role | `AuthFailureResponses.CODE_ACCESS_DENIED` 常量保留，FilterChain **未**配 AccessDeniedHandler / 角色 |

## Approach

- `spring-boot-starter-oauth2-resource-server`；`issuer-uri` 或 `jwk-set-uri` 必填（`ApiSecurityStartupValidator`）
- Validator：`JwtTimestampValidator(clock-skew-seconds)` + `iss ∈ resolveValidIssuers()`
- 失败信封：畸形/缺 Bearer → HTTP 401 `code=401`；验签/iss/过期 → HTTP 401 `code=4010`；`X-Api-Culture` 决定 en/zh message
- `user_id` = JWT `sub`（`CurrentUserIdAccessor`）

## Non-goals

Introspection、role→4030、fixed token 双轨、把 JWKS 放 Redis。
