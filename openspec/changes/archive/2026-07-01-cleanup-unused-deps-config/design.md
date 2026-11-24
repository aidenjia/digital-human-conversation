# Design: cleanup-unused-deps-config

## 依赖审计结果

| 依赖 | 代码引用 | 处置 |
|------|----------|------|
| spring-boot-starter-web | 全项目 | 保留 |
| spring-boot-starter-actuator | management 端点 | 保留 |
| spring-boot-starter-validation | `@Valid` DTO | 保留 |
| agora-rest-client-core | `ConvoaiClientFactory`、`ConvoaiService` | 保留 |
| authentication | `AgoraRtcTokenService` → `RtcTokenBuilder2` | 保留 |
| okhttp | `AgoraRestClient`（此前由已删依赖传递，现显式声明） | **保留（新增显式）** |
| jasypt-spring-boot-starter | `application-local.yml` ENC + `JasyptEncryptCommand` | 保留 |
| tls-sig-api-v2 | 无 | **删除** |
| tencentcloud-sdk-java-common | 无 | **删除** |
| tencentcloud-sdk-java-trtc | 无 | **删除** |
| commons-codec | 无（`java.util.Base64`） | **删除** |
| redisson | 无 Bean / 无 import | **删除** |

## 配置清理

| 文件 | 项 | 处置 |
|------|-----|------|
| `application.yml` | `logging.level.org.springframework.ai` | 删除 |
| `application-dev.yml` | `springdotenv.enabled` | 删除整块 |
| `application-test.yml` (main) | `com.jupiter.digital.human.agent` logger | 改为 `con.aidenjia.digitalhuman.conversation` |
| `application-local.yml` | `redis:` 块 | 删除（文件 gitignore，本地同步） |
| `src/test/resources/application-test.yml` | `springdotenv` | 删除 |
| `pom.xml` | 注释 TRTC 依赖、repositories | 删除注释 |

## README

- 日志行：去掉 `logback-spring.xml` 引用，改为「Spring Boot 默认 Logback + `application*.yml` 结构化 stdout」

## 验证

```bash
mvn verify
mvn dependency:analyze -DignoreNonCompile=true  # 可选，确认无新增未使用声明
```

## 风险

- 无运行时行为变更；若 CI 环境误依赖已删 Tencent SDK 将提前在编译期暴露（当前已无引用）。
