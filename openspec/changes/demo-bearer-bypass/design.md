## Context

ConvoAI join/leave 当前经 Spring OAuth2 Resource Server 本地 JWKS 验签 Users JWT，`sub` 驱动 Lock4j、陪伴偏好与 leave ownership。演示无法便捷获取真实 token，需要固定 Bearer 旁路，同时 UE5 仍从 `user_companion_config` 读配置。

## Goals / Non-Goals

**Goals:**
- 开关开启时，仅 `POST|DELETE /api/convoai/agents` 接受配置固定 Bearer，合成 `sub=demo-user-id`
- 真 JWT 仍可双轨访问该路径
- 其它 `/api/**` 不接受 demo token
- UE5 靠库种子（preset），业务编排零改
- 错误信封保持 401 / 4010

**Non-Goals:**
- 不放开陪伴配置等其它 API 的 demo 鉴权
- 不改 Agora REST / RTC Token
- 不引入 custom voice 演示种子
- 不为演示新增中间件

## Decisions

1. **Filter + BearerTokenResolver**：匹配路径且 token 相等时注入 `JwtAuthenticationToken`，并让 resolver 返回 null 跳过 JWKS；优于 permitAll 手写。
2. **配置**：`api.security.demo-token-enabled` / `demo-token` / `demo-user-id`；dev yml 明文（用户明示）；prod 默认关。
3. **UE5 数据**：`user_id=demo-user-id` + `voice_source=preset` 种子；无代码分支。
4. **规格**：delta `openid-jwt-validation` 增加可选 demo 旁路条款。

## Risks / Trade-offs

- [明文 token 进 Git] → 仅 dev；prod 不写；演示结束后可轮换
- [误开 prod] → prod yml 显式 false；启动校验 enabled 时必填 token/userId
- [demo token 打到其它 API] → 路径过滤器不注入，JWKS 失败 4010

## Migration Plan

1. 合并代码；dev 配 token；库插种子行
2. 客户端 join/leave 换固定 Bearer
3. 回滚：`demo-token-enabled=false` 或关 profile 覆盖

## Open Questions

None.
