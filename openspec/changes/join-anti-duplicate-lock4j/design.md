## Context

- 部署 `replicas: 2`，已有 Redisson（`digital-human.redis.*`，NameMapper 前缀 `digital-human:`）
- OkHttp 读超时默认 **30s**；join 含 Agora REST + 配置编排 + session warm
- 用户要求：先用 lock4j；**expire 必须合理**

## Goals / Non-Goals

**Goals:**

- join 同用户互斥（进行中第二请求 409）
- 定制音色「提交制作」同用户互斥（防止连点多建腾讯训练任务）
- expire 覆盖最坏执行时间，避免锁提前过期双进房 → **落地修正**：join 不写有限 `expire`，走 Redisson watchdog
- 错误对齐 `ApiResponse`；配置外置

**Non-Goals:**

- 成功响应缓存回放（真正幂等）
- lock4j 用于 leave / 语料上传 / 场景图（陪伴·次元启动保存 upsert 后续已加锁，见代码）
- 引入 Zookeeper / RedisTemplate 第二套客户端

## Decisions

1. **依赖**：`lock4j-redisson-spring-boot-starter` **2.2.7**；注入现有 `RedissonClient`
2. **挂载点**：
   - `ConvoaiController.join`（`name=convoai-join`）
   - `CustomVoiceController.submit`（`name=custom-voice-submit`）
   - 均在 HTTP 入口；避免 Service 自调用切不中
3. **Key**：`keys=@currentUserIdAccessor.requireUserId()`（按 JWT `sub`）
4. **TTL 策略（关键）**：
   - `acquire-timeout: 0` — 防连点不等待排队
   - **join（及当前其它 `@Lock4j`）不写 `expire`**：Redisson `renewal()=true` 时注解默认 `-1` 不会回落到 `lock4j.expire`，`tryLock(wait, -1)` 走 **watchdog 续期**，覆盖 join 超过 30s 读超时的路径
   - **禁止**在 join 上写正数 `expire`（含 `60000`）：有限 lease 会关掉 watchdog
   - `lock4j.expire` / `LOCK4J_EXPIRE_MS` 管不到未写 `expire` 的方法
   - `autoRelease: true`（默认）— 正常结束立即释放；进程崩溃靠 watchdog 超时兜底
5. **失败策略**：自定义 `LockFailureStrategy` 抛 `ApiException(409, ...)`；`GlobalExceptionHandler` 可兼收 `LockFailureException`
6. **测试**：策略单测；Controller 切片在无 Lock 切面时行为不变；可选验证 409 映射

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| 误以为已做幂等 | README / spec 明确「防连点，成功后重试仍可能再 join」 |
| 有限 expire 关掉 watchdog | join **不写** `expire`；文档禁止把 `LOCK4J_EXPIRE_MS` 当成 join 旋钮 |
| Redis 不可用 | 与现网一致依赖 Redis；抢锁失败路径测 409，不另做 fail-open（本期） |
| WebMvcTest 未加载 AOP | 切片测 happy path；锁失败测 Strategy / Handler |

## Migration Plan

1. 发版带 lock4j；join 不写注解 `expire`（Redisson 续期）
2. 客户端将 409 视为「进行中，稍后重试」
3. 回滚：去注解或降依赖发版

## Open Questions

无。
