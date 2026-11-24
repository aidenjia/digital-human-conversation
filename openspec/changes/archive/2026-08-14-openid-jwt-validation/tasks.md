## 1. Resource Server

- [x] 1.1 引入 OAuth2 Resource Server；删除固定 Bearer / `ApiAuthFilter`
- [x] 1.2 `NimbusJwtDecoder`：`jwk-set-uri` 或 issuer discovery；`validateType(false)` 以支持 `at+jwt`
- [x] 1.3 `iss` 白名单 + `exp` clock skew；配置外置，无明文密钥入库

## 2. HTTP 契约

- [x] 2.1 受保护：convoai / companions / companion-voices / user-companion-configs / dimension-voices / user-dimension-configs
- [x] 2.2 公开：`/`、`/api/echo`、health、prometheus、OPTIONS
- [x] 2.3 缺/畸形 Bearer → 401/`code=401`；验签失败 → 401/`code=4010`；i18n message
- [x] 2.4 `CurrentUserIdAccessor`：`sub` 为 `user_id`

## 3. Tests and docs

- [x] 3.1 `JwtResourceServerAuthTest`：缺 token / 无效 token / 合法 / additional issuer / zh message
- [x] 3.2 陪伴与次元 API Auth 切片
- [x] 3.3 README / 客户端文档 Bearer 约定
