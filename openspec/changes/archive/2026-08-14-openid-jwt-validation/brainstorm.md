## Design Summary

将 `digital-human-conversation` 的过渡固定 Bearer（`ApiAuthFilter` 字符串全等）**直接替换**为 OpenID Connect provider OpenID JWT 本地校验（JWKS 验签 + `iss` + `exp`），**不**调用 introspection，**不**做 role 校验，**不**保留 fixed / jwt 双轨。

JWT 路径采用 Spring OAuth2 Resource Server（`NimbusJwtDecoder`），鉴权失败写入现有 `ApiResponse` 信封，错误码 `401` / `4010`。

集群 `replicas=2`、无 Redis：JWKS 使用框架进程内缓存即可。

## Alternatives Considered

### Alternative A: 仅扩展现有 ApiAuthFilter 手写 JWT 校验

- **Approach**: 在 `ApiAuthFilter` 内拉 JWKS、验签、校 `iss`/`exp`
- **Pros**: 表面改动面小
- **Cons**: kid 轮换、多 issuer、时钟偏移需自研；与指南示例不一致
- **Why not chosen**: 正式 Resource Server 场景不应自研验签核心路径

### Alternative B: 全面切换为 Spring OAuth2 Resource Server（选定）

- **Approach**: 引入 `spring-boot-starter-oauth2-resource-server`，废弃 `API_FIXED_TOKEN`；受保护路由一律 JWT
- **Pros**: 与指南一致；标准、可测；无双轨复杂度
- **Cons**: 客户端与 CD 需同时切换到 Users access token
- **Why chosen**: 产品确认不需要过渡 fixed token、不做 role

### Alternative C: 配置开关 `fixed | jwt`

- **Approach**: 双模开关灰度
- **Pros**: 可回滚
- **Cons**: 测试矩阵与配置面加倍
- **Why not chosen**: 产品明确不需要保留现有固定 token

## Agreed Approach

采用 **Alternative B**：仅 JWT Resource Server。

必选校验：签名、`iss` ∈ ValidIssuers、`exp`（clock skew 默认 60s）。  
本期 **不做** `role` → `4030`。

公开路径：`GET /`、`POST /api/echo`、actuator health/prometheus、CORS `OPTIONS`。  
受保护路径：`/api/convoai/**`。

配置外置：`issuer-uri` / `jwk-set-uri` / `additional-issuer-uris` / `clock-skew-seconds`；禁止提交明文密钥。

## Key Decisions

1. **不做 introspection**；只本地 JWT 校验。
2. **不做 role**；不实现 authorities converter / AccessDeniedHandler（4030）。
3. **不做 fixed 逃生通道**；删除 `ApiAuthFilter`、`API_FIXED_TOKEN`、`api.security.mode`。
4. **错误码**：缺/畸形 Bearer → HTTP 401 + `code:401`；验签/iss/过期失败 → HTTP 401 + `code:4010`。
5. **成功响应不变**。
6. **JWKS**：进程内缓存；不引入 Redis。
7. **日志**：禁止打印 Authorization / access token。

## Open Questions

1. Prod CN AdditionalIssuer（`https://issuer.example.test/`）是否由 CD 按区域注入？（机制已支持）
2. 客户端切 Users token 的联调窗口与 Apifox 模板更新负责人？
