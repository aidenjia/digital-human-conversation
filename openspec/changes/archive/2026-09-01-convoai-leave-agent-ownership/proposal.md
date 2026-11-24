## Why

已认证用户只要知道他人的 `agentId`，即可成功 `DELETE /api/convoai/agents` 停掉他人 ConvoAI 会话（IDOR）。产品要求 leave 只能停本人 join 创建的 agent。

## What Changes

- join 声网成功后：Redis 写入 `agentId → JWT sub`（TTL 可配）
- leave 前：用当前用户比对映射；不匹配 / 无记录 / 读失败 → **404**，不调 Agora
- leave 成功后删除映射
- 单测覆盖：本人 leave 成功、他人 leave 404、无映射 404
- README 补充 leave 归属语义

## Capabilities

### New Capabilities

- `convoai-leave`: leave 必须校验 agent 归属；越权/未知 agentId 返回 404

### Modified Capabilities

- （无；join 侧仅增加写映射副作用，不改 join HTTP 契约）

## Impact

- API：他人 leave / 无映射 leave 从「可成功」变为 404（破坏性：收紧授权）
- 依赖：无新增；复用 Redisson
- 配置：可选 `convoai.agent-ownership-ttl`（或等价）；默认 1 天
- CD：无新中间件
- 自动测试：已有 leave 越权用例应变绿
- Non-goals：Postgres 会话表、leave 加锁、孤儿 agent 扫描
