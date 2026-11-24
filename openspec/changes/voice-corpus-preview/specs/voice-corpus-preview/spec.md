## ADDED Requirements

### Requirement: Issue presigned corpus read URL by object key

系统 SHALL 提供 `GET /api/custom-voices/corpus-url`（JWT Bearer）。Query MUST 含非空 `corpusUri`（COS 对象键，MUST NOT 为 `http`/`https` URL）。系统 MUST 校验该键属于当前 JWT `sub`（对象键前缀 MUST 匹配配置的 `keyPrefix` + `/{userId}/`）。校验通过后 MUST 使用服务端 COS 凭证生成 **GET** 预签名 URL，TTL MUST 取自配置（默认 300 秒）。成功时 `data` MUST 含 `url`（预签名完整 URL）与 `expiresAt`（与签名过期一致的时间，ISO-8601）。系统 MUST NOT 向客户端下发写权限临时凭证或 STS。

#### Scenario: Owner gets temporary read URL

- **WHEN** 已鉴权用户请求本人前缀下的合法 `corpusUri`
- **THEN** 系统 MUST 返回 `ApiResponse` 且 `data.url` 与 `data.expiresAt` 均非空

#### Scenario: Missing corpusUri fails

- **WHEN** 请求缺少 `corpusUri` 或值为空白
- **THEN** 系统 MUST 返回 400

#### Scenario: Foreign corpusUri is rejected

- **WHEN** `corpusUri` 不属于当前用户前缀
- **THEN** 系统 MUST 拒绝签名（400 或 403）且 MUST NOT 返回可用预签名 URL

#### Scenario: Unauthenticated request fails

- **WHEN** 未携带有效 JWT 调用 corpus-url
- **THEN** 系统 MUST 拒绝未认证请求

---

### Requirement: Issue presigned corpus read URL by businessCode

系统 SHALL 提供 `GET /api/custom-voices/{businessCode}/corpus-url`（JWT Bearer）。系统 MUST 按 `(user_id=sub, business_code)` 定位生产单；找不到或不属于当前用户时 MUST 返回 403 或 404。若生产单 `corpus_uri` 为空，MUST 返回 400。否则 MUST 对该 `corpus_uri` 按与对象键接口相同的预签名规则签发，成功 `data` MUST 含 `url` 与 `expiresAt`。

#### Scenario: Owner polls corpus URL by businessCode

- **WHEN** 所有者请求本人生产单且 `corpus_uri` 非空
- **THEN** 系统 MUST 返回临时读 `url` 与 `expiresAt`

#### Scenario: Empty corpus_uri fails

- **WHEN** 生产单存在但 `corpus_uri` 为空
- **THEN** 系统 MUST 返回 400

#### Scenario: Non-owner cannot get corpus URL

- **WHEN** 用户请求不属于自己的 `businessCode`
- **THEN** 系统 MUST 返回 403 或 404

---

### Requirement: Corpus preview uses unified envelope and safe logging

所有语料试音（临时读 URL）接口 MUST 使用统一 `ApiResponse` 信封，MUST 要求 JWT Bearer。日志 MUST NOT 输出完整预签名 URL、Authorization、COS SecretKey。COS 密钥或桶缺失时 MUST 以配置错误失败（503 或项目等价）。

#### Scenario: Missing COS configuration fails

- **WHEN** COS 未配置且执行预签名
- **THEN** 系统 MUST 以配置错误失败且 MUST NOT 返回伪造 URL
