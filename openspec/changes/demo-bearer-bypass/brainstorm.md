## Design Summary

演示场景下，仅 `POST|DELETE /api/convoai/agents` 可用配置的固定 Bearer 跳过 Users JWKS 验签，并合成 `sub=demo-user-id`。UE5 配置不改 Java：库表预置该 userid 的 `user_companion_config`（`voice_source=preset`）。其它 `/api/**` 仍走原 JWT。

## Alternatives Considered

### Alternative A: 共享密钥 + 合成 Jwt（路径限定）
- **Approach**: 开关 + demo-token；仅 convoai agents 路径匹配后注入 `JwtAuthenticationToken`，BearerResolver 跳过 JWKS
- **Pros**: Service/ownership/Lock4j 零改；可双轨真 JWT；范围可控
- **Cons**: 固定串泄露即冒充 demo 用户；与「仅 JWKS」活规格冲突（须 delta）

### Alternative B: join/leave permitAll + Controller 手写比对
- **Approach**: 两路由匿名，Controller 内比 token 并手工设 userId
- **Pros**: 看似短
- **Cons**: 破坏统一 Security；仍要补 `sub`；易漏 leave
- **Why not chosen**: 分层与契约更差

### Alternative C: 预签发长寿命演示 JWT
- **Approach**: 仍走 JWKS，客户端用长 TTL JWT
- **Pros**: 零旁路代码
- **Cons**: 依赖 issuer/JWKS；不满足「不走当前校验」
- **Why not chosen**: 演示摩擦仍在

## Agreed Approach

Alternative A + 库表种子（preset）。dev 配置明文写入 `application-dev.yml`（用户明示）；prod 默认关闭且不写 token。

## Key Decisions

- 范围：仅 `/api/convoai/agents`（A）
- `demo-user-id` 默认 `demo-ue5`，与种子 `user_id` 一致
- UE5：`voice_source=preset` 种子足够；业务代码不改
- 错误码：缺头 401 / 错 token 4010，不新增码
- Token 明文进 `application-dev.yml`（演示优先，接受与仓库脱敏规范冲突）

## Open Questions

- None（已确认）
