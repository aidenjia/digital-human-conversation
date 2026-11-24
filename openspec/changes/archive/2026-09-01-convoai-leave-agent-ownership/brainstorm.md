## Design Summary

leave（`DELETE /api/convoai/agents`）当前只认证、不校验「该 `agentId` 是否由当前 JWT `sub` 在 join 时创建」，导致持有他人 `agentId` 的登录用户可停掉他人会话（IDOR）。本期在 join 成功后于 Redis 登记 `agentId → userId`，leave 前比对；不匹配或无记录 → HTTP 404，不调声网。复用现有 Redisson / NameMapper，不落 Postgres。

## Alternatives Considered

### Alternative A: Redis `agentId → userId`（选定）
- **Approach**: join 成功后写 Redis；leave 校验归属后调 Agora；成功后删 key；TTL 可配且 ≥ idle-timeout
- **Pros**: 跨副本；与 companion/dimension cache 同构；改动面小；无新中间件
- **Cons**: 依赖 Redis；写失败/发版前旧 agent 无映射时本人 leave 可能 404（靠 idle）
- **Why chosen**: 产品确认必须归属校验；环境已有 Redis；对齐场景图越权 404

### Alternative B: Postgres 会话表
- **Approach**: join/leave 落库 agent 归属
- **Pros**: 可审计、不依赖 Redis TTL
- **Cons**: YAGNI；当前 join/leave 刻意不落库；迁移成本高
- **Why not chosen**: 超出本期；与现有架构冲突

### Alternative C: 文档化「agentId 即能力令牌」+ 测试 xfail
- **Approach**: 不改服务，接受 IDOR
- **Pros**: 零改动
- **Cons**: 与产品「必须只能停本人 agent」冲突
- **Why not chosen**: 已否决

## Agreed Approach

Alternative A：Redis 短生命周期归属映射 + leave fail-closed（404）。

## Key Decisions

1. **错误码**：无映射 / 不匹配 / Redis 读失败 → **HTTP 404** + `ApiResponse`（对齐场景图，不泄露归属细节）
2. **join 写 Redis 失败**：**不挡 join**（Agora 已创建）；打 error 日志；该会话 leave 可能 404，靠 `idle-timeout`
3. **Key**：逻辑 key `convoai-agent:{agentId}`（经 NameMapper 成 `digital-human:convoai-agent:{agentId}`），value = JWT `sub`
4. **TTL**：可配，默认 1 天（与 companion cache 一致，且 > 默认 idle 300s）
5. **无 Redisson（NoOp）**：leave **拒绝**（404）——本地/测试若无 Redis 则无法安全放行 leave
6. **leave 成功后删 key**；Agora leave 失败不删（可重试）
7. **Non-goals**：会话落 Postgres、leave 加锁、孤儿扫描、成功响应幂等

## Open Questions

无（用户已确认方案）。
