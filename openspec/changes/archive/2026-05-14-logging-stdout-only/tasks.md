## 1. Logback 与 Spring 配置

- [x] 1.1 修改 `src/main/resources/logback-spring.xml`：移除 `FILE_ALL`/`FILE_ERROR` 与仅文件的 `springProfile`，根 logger 全 profile 仅引用 `CONSOLE`；删除不再使用的 `springProperty`（`LOG_PATH`、`APP_NAME` 若仅服务文件 appender）
- [x] 1.2 更新 `src/main/resources/application.properties`：移除或注释废弃的 `logging.file.path` / `LOG_PATH` 说明，避免与 stdout 策略矛盾
- [x] 1.3 确认 `application-prod.properties` 无依赖文件日志的条目；若有则改为与 stdout 一致

## 2. 容器与文档

- [x] 2.1 更新根目录 `Dockerfile`：去掉 `logs/archive` 目录创建与 `chown`、去掉 `VOLUME ["/app/logs"]` 及仅针对文件日志的注释；保证 `app.jar` 与运行用户仍可用
- [x] 2.2 更新 `README.md`：配置表与 Docker 示例去掉日志目录/命名卷说明，写明日志由标准输出采集及 **BREAKING** 提示

## 3. 验证

- [x] 3.1 本地执行 `mvn verify`（或项目约定测试命令）通过
- [x] 3.2 分别以默认 profile 与 `prod` 启动，确认控制台有日志且无因缺少日志目录导致的启动失败
