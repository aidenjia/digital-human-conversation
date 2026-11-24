## Why

客户端可能连点或并发调用 join，双副本下会重复创建 Agora agent。本期用 lock4j 做用户级互斥防连点；**不**宣称完整 HTTP 幂等回放。

## What Changes

- 引入 `lock4j-redisson-spring-boot-starter`（复用现有 `RedissonClient`）
- `join` 声明 `@Lock4j`：key = 用户 `sub` + 操作名；`acquireTimeout=0`；**不写**注解 `expire`（Redisson watchdog 续期）
- 抢锁失败 → 409（`ApiResponse`）
- README 写明：未写 `expire` 时 `LOCK4J_EXPIRE_MS` 管不到 join

## Capabilities

### New Capabilities

- `convoai-join-anti-duplicate`: join 进行中同一用户互斥；抢锁失败 409

### Modified Capabilities

- （无活规格 delta 名变更；行为补充在 delta spec）

## Impact

- API：并发/连点 join 可能 409（新行为）
- 依赖：lock4j
- 配置：`lock4j.*`
- CD：无新中间件（沿用 Redis）
- 同批扩展：`POST /api/custom-voices`（提交制作）同用户进行中互斥
- Non-goals：成功响应回放、`Idempotency-Key`、leave / 语料上传 / 场景图上传 / 陪伴·次元启动保存 upsert 加锁
