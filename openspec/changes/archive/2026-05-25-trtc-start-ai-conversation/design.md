## Context

本服务为 Spring Boot 3.x Java 应用，已约定业务 API 位于 `con.aidenjia.digitalhuman.conversation.api` 且类级映射 `/api/v1`。腾讯云 TRTC 提供 **StartAIConversation**（云 API 3.0），用于在指定房间内以机器人身份启动 AI 对话任务；机器人进房需 **UserId + UserSig**（与控制台/SDKAppId、密钥体系一致）。产品侧要求：敏感信息走环境变量、过渡期 Header 鉴权、机器人 UserId 全局唯一、地域固定广州、LLM/TTS 配置服务端固定、日志不得输出完整 LLMConfig、Maven 中腾讯云 SDK 固定版本。

## Goals / Non-Goals

**Goals:**

- 提供单一「开始 AI 对话」HTTP 接口，将合法请求转换为 `StartAIConversationRequest` 并成功调用腾讯云 TRTC Java SDK。
- 使用环境变量注入云 API 密钥、UserSig 密钥、LLM API Key 等；classpath 或配置文件承载非敏感的 LLM/TTS 固定模板。
- Header 鉴权与客户端 UserId 解析规则清晰、可测、可演进为正式鉴权（JWT 等）。
- 可观测性上可记录 `TaskId`、`RequestId`、`RoomId`、`SdkAppId`、机器人/客户端 UserId（非敏感），**绝不**记录完整 `LLMConfig` 字符串或 API Key。

**Non-Goals:**

- 停止对话、暂停/恢复、任务查询与回调处理（后续变更）。
- 多地域路由、前端动态拼装 LLM/TTS JSON。
- 将 TRTC 全量能力或数字人 Avatar 等需单独加白的能力纳入本期必选范围。

## Decisions

1. **API 路径与控制器归属**  
   - **决策**：在 `api` 包新增控制器，路径形如 `POST /api/v1/trtc/ai-conversations/start`（与现有显式 `/api/v1` 一致）。  
   - **理由**：符合 `api-routing-policy`，与 `/api/v1/echo` 并列，不引入按包自动前缀。

2. **鉴权与客户端 UserId**  
   - **决策**：使用可配置 Header 名（默认如 `X-Api-Token`）；值为 **`<trtcClientUserId>|<apiFixedSecret>`**（首个 `|` 分隔，`|` 不得出现在 UserId 内）；服务端校验 `<apiFixedSecret>` 与环境变量 `API_FIXED_TOKEN`（或配置绑定）一致后，将 `<trtcClientUserId>` 写入 `AgentConfig.TargetUserId`。  
   - **备选**：双 Header（固定密钥 + UserId）更简单但不符合「从 token 解析 userId」的表述；JWT 留待后续。  
   - **理由**：单 Header 兼顾过渡期固定密钥与可变的 TRTC 用户标识，便于后续改为签名 token 而不改 Header 名。

3. **机器人 UserId**  
   - **决策**：由 `TRTC_ROBOT_USER_ID`（或 `trtc.robot.user-id`）注入，部署内全局唯一；不进请求体。  
   - **理由**：满足「机器人 UserId 全局唯一」且避免客户端伪造。

4. **UserSig**  
   - **决策**：使用已有 `tls-sig-api-v2`（`TLSSigAPIv2`）对机器人 `UserId` 生成 UserSig；SdkAppId/密钥使用可配置的 UserSig 专用变量，默认可与云 API 密钥同源（与现有 `application.properties` 占位一致）。  
   - **理由**：与 TRTC 官方文档一致，依赖已在仓库。

5. **TRTC 云客户端**  
   - **决策**：使用 `tencentcloud-sdk-java-trtc` **固定版本**（与传递依赖 `common` 对齐，如 `3.1.1289`），`Credential` + `ClientProfile` + `HttpProfile` 指向 `trtc.tencentcloudapi.com`，`region` 固定 `ap-guangzhou`。  
   - **理由**：满足构建可复现与用户要求。

6. **LLMConfig / TTSConfig 组装**  
   - **决策**：从 `classpath:trtc/llm-config.json` 与 `classpath:trtc/tts-config.json` 读取 JSON；LLM 在内存中合并 `APIKey`（及必要时 `APIUrl`）自环境/配置；TTS 整段固定字符串上传，不接受请求体字段覆盖。  
   - **理由**：满足「固定写在配置文件」「TTS 不由前端传递」；密钥不进 Git。

7. **STTConfig 与其它可选字段**  
   - **决策**：本期若文档未强制，可置空或省略；若联调失败再补最小 STT 配置。  
   - **理由**：降低首期复杂度，以联调反馈迭代。

8. **错误与配置缺失**  
   - **决策**：未配置完整 TRTC/LLM 环境时，开始接口返回 **503** 或带明确 `ProblemDetail` 的 **422**（团队择一并在实现中统一）；腾讯云 SDK 异常映射为 **502** 且不向客户端透传原始堆栈细节。鉴权失败 **401**。  
   - **理由**：与 Problem Details 一致且语义清晰。

## Risks / Trade-offs

- **[Risk] 单 Header `userId|secret` 泄露即同时暴露用户标识与 API 密钥** → **缓解**：仅 HTTPS 部署、短期密钥轮换、后续改为 JWT。  
- **[Risk] 日志误打 LLMConfig** → **缓解**：代码评审 + 结构化日志字段白名单；禁止 `log.info("{}", llmConfig)`。  
- **[Trade-off] 固定广州地域** → 跨地域延迟或合规问题由部署与网络规划承担，后续再抽象 Region。  
- **[Risk] StartAIConversation 参数演进** → **缓解**：封装单服务类，升级 SDK 小版本时集中回归。

## Migration Plan

1. 在目标环境注入所需环境变量（云密钥、SdkAppId、Robot UserId、API_FIXED_TOKEN、LLM API Key 与 URL 等）。  
2. 发布新版本应用；先对灰度房间验证 `StartAIConversation` 返回 `TaskId`。  
3. 回滚：回退镜像/制品；不涉及数据库 schema 变更则 rollback 仅为重新部署旧版本。

## Open Questions

- STT 是否在本环境为必填；若必填，最小 `STTConfig` JSON 形态需与腾讯云文档对齐后补入设计与 spec。  
- `SessionId` 幂等与重试策略是否由调用方保证（建议文档化，本期可不实现服务端去重）。
