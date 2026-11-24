## 1. Properties & startup

- [ ] 扩展 `ApiSecurityProperties`：`demoTokenEnabled`、`demoToken`、`demoUserId`
- [ ] `ApiSecurityStartupValidator`：enabled 时校验 token/userId 非空
- [ ] `application.yml` 默认 false/空；`application-dev.yml` 明文启用；`application-prod.yml` 显式 false

## 2. Security wiring

- [ ] 实现 `DemoBearerAuthenticationFilter`（仅 agents 路径匹配注入 `JwtAuthenticationToken`）
- [ ] 自定义 `BearerTokenResolver`：demo 已认证时返回 null
- [ ] 接入 `ResourceServerSecurityConfig`

## 3. Tests & docs

- [ ] `JwtResourceServerAuthTest`（或新测）：enabled 正确 token / 错 token / 其它路径 / disabled / 真 JWT
- [ ] README 演示说明 + 种子 SQL 示例
- [ ] `mvn spotless:apply` && `mvn verify`
