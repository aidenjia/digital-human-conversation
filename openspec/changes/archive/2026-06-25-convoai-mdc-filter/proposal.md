## Why

ConvoAI 网关已在日志消息中用 `deviceId=`、`channel=` 等字段串联排障，但尚未写入 SLF4J MDC。在已启用 Logstash 结构化输出（`logging.structured.include: [mdc, request]`）的前提下，运维无法通过 `mdc.deviceId` 过滤单次 join/leave 全链路日志，与 digital-human-agent 的 `RequestLogContext` 实践不一致。`.cursor/rules/application-logging.mdc` 已将「`/api/convoai/**` MDC Filter」列为后续增强，现补齐以降低联调与生产检索成本。

## What Changes

**ConvoAI 请求关联（MDC）**
- From: 关联字段仅出现在日志消息正文；无请求级 MDC。
- To: `/api/convoai/**` 经 `ConvoaiMdcFilter` 写入 MDC；`ConvoaiService` enrich `channel`/`userId`/`agentId`；请求结束 `MDC.clear()`。
- Reason: 结构化日志检索与 agent 项目对齐。
- Impact: 非破坏性；HTTP 契约不变；日志文本可能减少重复 `key=value` 字段。

**实现组件**
- 新增 `trtc.support.ConvoaiLogContext`
- 新增 `security.ConvoaiMdcFilter`（或 `logging` 包，实现放 `security` 与现有 Filter 并列）
- 更新 `WebSecurityConfig` 注册 Filter
- 更新 `ConvoaiService` / `application-logging.mdc`（去掉「后续增强」措辞）
- 集成测试 + 单测

## Capabilities

### New Capabilities

- `convoai-request-logging`: ConvoAI HTTP 请求的 MDC 绑定、enrich、清理与测试要求。

### Modified Capabilities

- `application-logging`: 补充「ConvoAI 路由须经 MDC Filter 建立关联上下文并清理」的要求。

## Impact

- **代码**: `security/ConvoaiMdcFilter.java`、`trtc/support/ConvoaiLogContext.java`、`config/WebSecurityConfig.java`、`trtc/ConvoaiService.java`
- **测试**: `ConvoaiMdcFilterTest` 或 `WebApiFoundationIT` 扩展；`ConvoaiLogContextTest`
- **文档**: `.cursor/rules/application-logging.mdc`（现状 → 已实现）
- **API**: 无变更
- **依赖**: 无新 Maven 依赖（Spring `ContentCachingRequestWrapper` 已有）
