## Why

定制音色语料存在私有 COS，前端无法用永久公网地址播放。提交前本地播 blob 可临时应付，但跨页回放、制作中/完成后重听仍需要服务端签发短时效读链接。生产能力已合入 develop，本期补齐「试音」读路径，与先前约定一致。

## What Changes

**语料临时读 URL**
- From: 仅有上传 PutObject / 服务端 GetObject（Detect）；无客户端可读链接
- To: JWT 接口返回 COS 预签名 GET URL（短 TTL）+ `expiresAt`
- Reason: 私有桶试音/回放
- Impact: 非破坏性；新增 API；前端可选接入

**COS Client**
- From: `putObject` + `getObjectBytes`
- To: 增加 `presignGet(objectKey, ttlSeconds)`
- Reason: 统一走 infrastructure，不在 Controller 拼签名
- Impact: 内部扩展；无对外破坏

**配置**
- From: `tencent.cos` 无预签名 TTL
- To: 增加 `presign-ttl-seconds`（默认 300）
- Impact: yml / 可选环境变量；CD 可不改即可用默认值

## Capabilities

### New Capabilities
- `voice-corpus-preview`: 定制语料私有桶临时读 URL（预签名试音/回放）API 与归属校验

### Modified Capabilities
- （无）`custom-voice-production` 生产契约不改；本能力独立增量。归档主规格可在实现后交叉引用，但不修改其 Requirements。

## Impact

- 代码：`TencentCosClient` / `RealTencentCosClient`、`TencentCosProperties`、`api.customvoice`（Controller + DTO）、`customvoice` 编排 Service
- API：新增 `GET /api/custom-voices/corpus-url`、`GET /api/custom-voices/{businessCode}/corpus-url`
- 文档：README 定制音色小节补试音约定
- 测试：归属失败 / 缺 token / 合法签名；COS 可用 Mockito
- 部署：replicas=2 无粘滞；沿用现有 COS 密钥；无需新中间件
