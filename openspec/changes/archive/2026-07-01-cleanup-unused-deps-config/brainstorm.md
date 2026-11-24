## Design Summary

项目已从腾讯云 TRTC 云 API 迁移为纯声网 ConvoAI HTTP 网关，但 `pom.xml` 与部分 YAML 仍保留 TRTC / Redis / spring-dotenv 等历史残留。本次变更以**最小 diff**移除未引用 Maven 依赖与无效配置，保留 ConvoAI 运行时仍需要的依赖（Spring Web/Actuator/Validation、Agora SDK、Jasypt 本地 ENC）。

## Alternatives Considered

### Alternative A: 仅移除 pom 依赖，不动 YAML
- **Approach**: 删除未使用的 Maven 坐标，配置文件原样保留
- **Pros**: 改动面最小；不影响本地 profile
- **Cons**: `redis:`、`springdotenv:`、`org.springframework.ai` 等死配置继续误导开发者
- **Why not chosen**: 清理目标包含「配置」，仅改 pom 不彻底

### Alternative B: 全量清理（含 Jasypt 与本地 ENC 工具）
- **Approach**: 移除 Jasypt、Redisson、Tencent 全家桶，本地密钥改明文或环境变量
- **Pros**: 依赖最少
- **Cons**: 破坏现有 `application-local.yml` + `ENC(...)` 本地开发流程；`JasyptEncryptCommand` 失效
- **Why not chosen**: Jasypt 仍被 local profile 与加密工具使用

### Alternative C: 依赖 + 配置同步清理（推荐）
- **Approach**: 移除 TRTC SDK、tls-sig、Redisson、commons-codec；清理无效 YAML；修正 README 中已不存在的 `logback-spring.xml` 描述
- **Pros**: 与当前 ConvoAI 架构一致；`mvn verify` 可验证；保留 Jasypt 本地能力
- **Cons**: 需同步 README；`application-local.yml` 在 `.gitignore` 中，需开发者本地手动删 redis 块（或在文档说明）
- **Why not chosen**: —（选定方案）

## Agreed Approach

采用 **Alternative C**：以代码引用与 `mvn dependency:analyze` 为据，删除无 import 的依赖；清理无对应 Bean/库的 YAML 键；保留 `jasypt-spring-boot-starter`、`agora-rest-client-core`、`authentication`。

## Key Decisions

| 项 | 决策 |
|----|------|
| `tls-sig-api-v2` | 移除（无 UserSig 生成代码） |
| `tencentcloud-sdk-java-*` | 移除（TRTC 云 API 已下线） |
| `redisson` | 移除（README 与 java-api-convoai 明确不使用 Redis） |
| `commons-codec` | 移除（代码使用 `java.util.Base64`） |
| `jasypt-spring-boot-starter` | **保留**（local profile `ENC(...)` + 测试加密工具） |
| `springdotenv` YAML | 删除（pom 无 spring-dotenv 依赖） |
| `org.springframework.ai` 日志级别 | 删除（无 spring-ai 依赖） |
| `application-test.yml` 错误 logger 包名 | 修正为 `con.aidenjia.digitalhuman.conversation` |
| pom 注释块 | 删除已注释的 TRTC 依赖与 repositories |

## Open Questions

- 无。`openspec/specs/trtc-*` 活规格为历史遗留，不在本次 scope（可另开 change 归档/下线）。
