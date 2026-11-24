## 1. COS presign infrastructure

- [ ] 1.1 `TencentCosProperties` 增加 `presignTtlSeconds`（默认 300）并在 `application*.yml` 占位
- [ ] 1.2 `TencentCosClient` 增加 `presignGet(objectKey, ttlSeconds)`；`RealTencentCosClient` 实现；缺配置 → ConfigurationException
- [ ] 1.3 单元测试：缺密钥/桶失败；TTL 传入行为（可用 mock COS 或抽可测方法）

## 2. Preview API

- [ ] 2.1 DTO `CorpusPreviewResponse(url, expiresAt)`；Service：按 `corpusUri` 校验归属后签名
- [ ] 2.2 Service：按 `businessCode` 查生产单归属 + `corpus_uri` 后签名
- [ ] 2.3 `CustomVoiceController`：`GET /corpus-url`、`GET /{businessCode}/corpus-url`；JWT + `ApiResponse`
- [ ] 2.4 拒绝 `http(s)` 形态的 `corpusUri`；空白 / 他人前缀 → 400/403

## 3. Tests & docs

- [ ] 3.1 API/Service 测试：无 token、缺参、他人 key、合法返回、空 corpus_uri、非所有者 businessCode
- [ ] 3.2 `mvn spotless:apply` 与 `mvn verify`
- [ ] 3.3 更新 README 定制音色小节：试音接口约定（入参/响应/过期重签）
