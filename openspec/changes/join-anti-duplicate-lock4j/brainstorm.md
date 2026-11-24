## Design Summary

本期用 [lock4j](https://github.com/baomidou/lock4j)（Redisson 执行器）为 `POST /api/convoai/agents`（join）提供**跨副本防连点 / 进行中互斥**。语义是：同一用户并发或短时重复 join，第二个请求抢不到锁 → **409**，**不**做成功响应回放。锁过期时间必须覆盖 join 最坏耗时，避免执行中锁提前失效导致双进房。

## Alternatives Considered

### Alternative A: lock4j `@Lock4j` + 现有 Redisson（选定）
- **Approach**: 引入 `lock4j-redisson-spring-boot-starter`；join 方法声明式加锁；key 绑定 JWT `sub`；`acquireTimeout=0`；**不写**注解 `expire`（Redisson watchdog 续期）；`LockFailureStrategy` → `ApiException(409)`
- **Pros**: 注解简单；复用已有 `RedissonClient`；满足「先按 lock4j 落地防连点」
- **Cons**: 不是 Stripe 式幂等；成功返回后锁释放，超时重试仍可能再 join
- **Why chosen**: 用户明确本期先用 lock4j；防连点与库定位一致

### Alternative B: 自研 Redis 占位 + 成功响应回放
- **Approach**: `Idempotency-Key` + COMPLETED 缓存
- **Pros**: 真正幂等
- **Cons**: 本期不做；与「先用 lock4j」冲突
- **Why not chosen**: 排期延后

### Alternative C: 仅本地 `synchronized` / 单机锁
- **Approach**: JVM 内互斥
- **Pros**: 无依赖
- **Cons**: `replicas: 2` 无效
- **Why not chosen**: 部署拓扑不满足

## Agreed Approach

lock4j + Redisson；仅 join；合理 `expire`；失败映射现有 `ApiResponse`；文档写明「防连点 ≠ 幂等回放」。

## Key Decisions

1. **范围**：仅 `POST /api/convoai/agents`；leave 不挂锁
2. **Key**：`convoai-join` + JWT `sub`（经现有 NameMapper 仍带 `digital-human:`）
3. **acquireTimeout**：`0`（不等待，连点立即失败）
4. **expire**：join **不写**注解 `expire`。Redisson `renewal()=true` 时默认 `-1` 走 watchdog，覆盖超过 30s 读超时的 join；写正数 `expire` 会关掉续期
5. **autoRelease**：默认 `true`（方法结束释放；进程崩溃靠 watchdog 超时兜底）
6. **错误**：409 + 现有信封
7. **后续**：若要真正幂等，另开 change 做响应回放，可与 lock4j 并存但有 key 时以回放为准

## Open Questions

无（用户已确认先用 lock4j，并要求 expire 合理）。
