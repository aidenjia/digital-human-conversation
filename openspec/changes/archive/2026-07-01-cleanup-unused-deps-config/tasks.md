# Tasks: cleanup-unused-deps-config

- [x] 1.1 从 `pom.xml` 移除未使用依赖：`tls-sig-api-v2`、`tencentcloud-sdk-java-common`、`tencentcloud-sdk-java-trtc`、`commons-codec`、`redisson`
- [x] 1.2 删除 `pom.xml` 中已注释的 TRTC 依赖与 `repositories` 块
- [x] 2.1 清理 `application.yml`：`org.springframework.ai` 日志级别
- [x] 2.2 清理 `application-dev.yml`：`springdotenv` 配置
- [x] 2.3 修正 `application-test.yml`（main）错误 logger 包名
- [x] 2.4 清理 `src/test/resources/application-test.yml`：`springdotenv`
- [x] 2.5 清理 `application-local.yml`：`redis` 块（本地 gitignore 文件）
- [x] 3.1 更新 `README.md` 日志/技术栈描述（移除 `logback-spring.xml`）
- [x] 4.1 运行 `mvn verify` 通过
