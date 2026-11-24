## MODIFIED Requirements

### Requirement: Protected APIs MUST validate Users OpenID JWT locally

系统 MUST 使用 Spring OAuth2 Resource Server 对受保护路径校验 OpenID Connect provider access token：JWKS 验签、`iss` ∈ 配置的 ValidIssuers（`issuer-uri` + `additional-issuer-uris`）、`exp`（`clock-skew-seconds` 默认 60）。系统 MUST NOT 调用 token introspection。系统 MUST NOT 按 role 拒绝请求（本期无 4030 路径）。Users token 头为 `typ=at+jwt` 时，解码器 MUST NOT 因 typ 不是 `JWT` 而失败。

受保护路径 MUST 包括：`/api/convoai/**`、`/api/companions`/`**`、`/api/companion-voices`/`**`、`/api/user-companion-configs`/`**`、`/api/dimension-characters`/`**`、`/api/dimension-voices`/`**`、`/api/user-dimension-configs`/`**`。下列路径 MUST 保持匿名：`GET /`、`/api/echo`、`/actuator/health`/`**`、`/actuator/prometheus`、CORS `OPTIONS /**`。

启动时 MUST 要求配置 `api.security.issuer-uri` 或 `jwk-set-uri` 至少其一。业务 `user_id` MUST 取自 JWT `sub`。

#### Scenario: Missing Authorization is 401

- **WHEN** 客户端调用受保护 POST 且无 `Authorization` 头
- **THEN** 系统 MUST 返回 HTTP 401 且信封 `code=401`

#### Scenario: Invalid token is 4010

- **WHEN** 客户端携带无法验签或非法的 Bearer token
- **THEN** 系统 MUST 返回 HTTP 401 且信封 `code=4010`

#### Scenario: Additional issuer is accepted

- **WHEN** token `iss` 属于 `additional-issuer-uris` 且签名有效
- **THEN** 系统 MUST 允许访问受保护资源

#### Scenario: Public echo does not require JWT

- **WHEN** 客户端调用 `POST /api/echo` 且未带 token
- **THEN** 系统 MUST NOT 因鉴权失败拒绝该请求

#### Scenario: Culture header localizes auth message

- **WHEN** 鉴权失败且请求 `X-Api-Culture: zh`
- **THEN** 信封 `message` MUST 为中文文案（无法鉴权访问 / 访问令牌已失效）

#### Scenario: Dimension characters requires JWT

- **WHEN** 未携带有效 JWT 调用 `GET /api/dimension-characters`
- **THEN** 系统 MUST 拒绝请求（401 或项目等价）
