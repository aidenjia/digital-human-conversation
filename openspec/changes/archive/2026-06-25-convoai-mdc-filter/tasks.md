## 1. ConvoaiLogContext

- [x] 1.1 新增 `trtc/support/ConvoaiLogContext.java`：MDC 键常量、`putIfText`、`bindJoinPreview`、`bindLeave`、`enrichAfterJoin`、`clear`
- [x] 1.2 单测 `ConvoaiLogContextTest`：bind/clear/enrich 行为

## 2. ConvoaiMdcFilter

- [x] 2.1 新增 `security/ConvoaiMdcFilter`：`CachedBodyHttpServletRequest`、解析 join/leave body 字段、OPTIONS 跳过、`finally` clear
- [x] 2.2 在 `WebSecurityConfig` 注册，url pattern `CONVOAI_PREFIX`，order `HIGHEST_PRECEDENCE + 12`
- [x] 2.3 单测或集成测试：join 后 MDC 含 `deviceId`；请求结束后 MDC 为空；`/api/echo` 不触发

## 3. Service 与日志去重

- [x] 3.1 `ConvoaiService.join`：enrich `channel`/`userId`/`agentId`；INFO 日志去掉与 MDC 重复的 `deviceId=` 等字段
- [x] 3.2 `ConvoaiService.leave`：确认 `agentId` 已在 Filter 绑定；日志保持生命周期句式

## 4. 文档与验证

- [x] 4.1 更新 `.cursor/rules/application-logging.mdc`：「后续增强」改为已实现说明
- [x] 4.2 `mvn verify` 全绿
