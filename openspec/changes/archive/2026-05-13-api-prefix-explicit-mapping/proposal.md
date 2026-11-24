## Why

去掉 `ApiPathPrefixConfiguration` 后，路径前缀不再由「按包名集中注册」隐式注入，减少一层 `WebMvcConfigurer` 心智负担；业务 URL 仍保持 **`/api/v1/**`**，由控制器显式声明即可。

## What Changes

- 删除 `ApiPathPrefixConfiguration`。
- `EchoController`（及后续 `api` 包控制器）在类上使用 **`@RequestMapping("/api/v1")`**（或等价显式路径），方法上仍只写资源段。
- 更新 `application.properties` 注释、`README` 中与「集中前缀配置」相关的描述。
- **`openspec/specs/api-routing-policy/spec.md`**：将「按包 PathMatch 前缀」改为「显式类级映射」要求。

## Capabilities

### New Capabilities

- （无）

### Modified Capabilities

- `api-routing-policy`：路径前缀实现方式从 `addPathPrefix` 改为显式 `@RequestMapping`。

## Impact

- 对外 HTTP 契约不变；新增 `api` 包控制器时须在类上写 `/api/v1`（或完整路径），否则不会自动带前缀。
