## Design Summary

为定制音色提供**语料试音（回放）**：私有 COS 桶内录音不能裸链长期访问；前端向本服务要**短 TTL 预签名 GET URL**，用该 URL 播放/下载；过期后重新申请。

本期范围锁定：

- **仅读临时 URL**（预签名 GET），不是上传凭证 / STS / 前端直传
- 对象键沿用现有 `corpusUri`（`{keyPrefix}/{userId}/{uuid}.ext`）
- 归属校验复用 `CustomVoiceCorpusKeys.belongsToUser`
- **不做**即时 TTS 合成试听、目录预设样音、改 join / 列表字段

部署：K8s `replicas: 2`；Postgres + Redis + 腾讯 COS 已有。预签名无状态，不依赖粘滞会话或 Redis。

## Alternatives Considered

### Alternative A: COS 预签名 GET（选定）
- **Approach**: JWT 接口校验对象键归属后，服务端用 COS SDK 生成短 TTL 预签名 URL；前端 `<audio>` 直连 COS。
- **Pros**: 对齐已定产品逻辑；双副本无状态；流量不经本服务；改动面小（扩 `TencentCosClient` + 1～2 API）。
- **Cons**: URL 在 TTL 内可被转发；前端需处理过期重签。
- **Why chosen**: 与先前明确约定一致；基础设施已有 COS Put/Get。

### Alternative B: 本服务代理流式下载
- **Approach**: `GET .../corpus` 由服务 GetObject 再 `application/octet-stream` 回传。
- **Pros**: 桶完全不对外；无签名 URL 泄漏面。
- **Cons**: 双副本吃带宽/超时；不如静态音讯；与「临时读链接」产品表述不符。
- **Why not chosen**: 成本高且偏离约定。

### Alternative C: 腾讯 TTS 即时合成试听
- **Approach**: 用训成 `VoiceType` 或预设音色合成固定文案返回音频。
- **Pros**: 真「音色效果」试听。
- **Cons**: 新 TTS Client、计费与延迟；与「播私有桶录音」不是同一需求。
- **Why not chosen**: 明确后置；本期只做语料回放。

## Agreed Approach

在 `api.customvoice` 增加语料临时读 URL API；`infrastructure.cos` 增加 `presignGet`；TTL 外置到 `TencentCosProperties`。上传与生产流程不变。

## Key Decisions

1. 主接口：`GET /api/custom-voices/corpus-url?corpusUri=`（JWT）；`data` 含 `url`、`expiresAt`（ISO-8601 UTC）。
2. 可选便捷接口：`GET /api/custom-voices/{businessCode}/corpus-url`——校验生产单归属后用库内 `corpus_uri` 签名（前端不必再持对象键）。
3. 归属：`corpusUri` 必须 `belongsToUser(keyPrefix, sub, corpusUri)`；`businessCode` 路径按 `(user_id=sub, business_code)` 定位，缺失 `corpus_uri` → 400。
4. TTL 默认 **300s**，配置键 `tencent.cos.presign-ttl-seconds`；响应 `expiresAt` 与签名过期一致。
5. 禁止日志输出完整预签名 URL / SecretKey；可记 objectKey、TTL、userId（脱敏策略同现有）。
6. 统一 `ApiResponse`；401/400/403|404/503 对齐现有 custom-voices。
7. Non-goals：TTS 试听、公开桶、STS 写凭证、改 companion/dimension 列表、改 join。

## Open Questions

- （已决）实现路径 = 预签名 GET，非代理流、非 TTS。
- （已决）默认 TTL = 300s，可配置。
- （实现时）COS SDK 预签名 API 与现有 `RealTencentCosClient` 客户端复用方式。
- （产品可选）若前端提交前始终本地播 blob，可只实现 `corpusUri` 查询接口；`businessCode` 路径仍建议一并做，便于制作中/完成后回放。
