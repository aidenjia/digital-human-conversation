## Context

- 部署：`replicas: 2`，无粘滞；已有 Postgres + Redis（Redisson，`digital-human:` NameMapper）
- join/leave 不落 Postgres；`agentId` 仅来自 Agora join 响应
- 场景图 delete 已按 `userId` 过滤，越权 → 404
- 自动测试已复现：用户 B leave 用户 A 的 `agentId` → HTTP 200

## Goals / Non-Goals

**Goals:**

- leave 仅允许停「当前 JWT `sub` 在 join 时登记」的 agent
- 跨副本一致（Redis）
- 错误对齐 `ApiResponse` + HTTP 404
- join 成功响应形状不变

**Non-Goals:**

- 会话长期落 Postgres
- leave 分布式锁
- 孤儿 agent 服务端主动清理
- join 成功响应幂等回放

## Decisions

1. **Store 接口**：`ConvoaiAgentOwnershipStore`（`bind(agentId, userId)` / `findOwner(agentId)` / `unbind(agentId)`）；包归属：接口可放 `trtc.support`，实现放 `infrastructure.redis`
2. **Key**：`convoai-agent:{agentId}` → 字符串 `userId`；TTL 默认 `Duration.ofDays(1)`，可经 `convoai.agent-ownership-ttl`（Duration/秒）外置
3. **编排**：
   - `ConvoaiService.join`：Agora join 成功后 `bind`（失败只打日志，不抛）
   - `ConvoaiService.leave`：`requireUserId` → `findOwner`；空或不匹配 → `ApiException(404, ...)`；匹配 → Agora leave → 成功后 `unbind`
4. **NoOp**：无 `RedissonClient` 时 NoOp：`findOwner` 恒 empty → leave 恒 404；`bind` no-op（与「无 Redis 不安全放行」一致）
5. **测试**：Mock store；缺映射 / 他人 / 本人路径；不测真 Redis 集成（可选）

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| join 后 Redis 写失败 → 本人 leave 404 | 文档说明；靠 idle-timeout；日志 error |
| 发版前已存在 agent 无映射 | 接受；靠 idle |
| Redis 宕机 leave 全 404 | 与 lock4j 同依赖 Redis；安全优先 fail-closed |
| NoOp 环境 leave 不可用 | 现网/local 均配 Redis；文档注明 |

## Migration Plan

1. 发版后新 join 均带映射；旧 agent 无映射 leave 变 404
2. 客户端将 leave 404 视为「无权或会话不存在」
3. 回滚：去掉校验与 bind（恢复 IDOR，不推荐）

## Open Questions

无。
