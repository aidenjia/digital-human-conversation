## Why

业务需要在服务端调用腾讯云 TRTC **StartAIConversation**（开始 AI 对话），为数字人/语音房场景拉起云端对话任务；敏感凭据不能入库或写死在仓库中，且需在落地前明确鉴权、配置来源与可观测性边界，避免误将 LLM 密钥或完整 LLM 配置写入日志。

## What Changes

- 新增 **`POST /api/v1/trtc/ai-conversations/start`**（或等价路径）：根据请求体中的房间信息调用腾讯云 **StartAIConversation**，返回任务标识（如 `TaskId`、`RequestId`）。
- **鉴权（过渡期）**：在指定 HTTP Header 中校验**固定 API 密钥**；**客户端 TRTC UserId** 由 Header 中 token 的约定格式解析（与后续 JWT 等鉴权方案可演进）；**机器人 UserId** 为部署级全局唯一，由环境/配置提供，不由前端指定。
- **地域**：云端 API 调用固定使用 **`ap-guangzhou`**（与配置默认值一致，不要求多地域列表）。
- **LLMConfig**：全部固定项写在**配置文件或 classpath JSON** 中；运行时仅合并环境变量中的敏感项（如 `APIKey`），**禁止**在日志中打印完整 `LLMConfig` 或含密钥的 LLM 载荷。
- **TTSConfig**：服务端固定模板（classpath JSON 或等价），**不由前端传递**。
- **依赖构建**：`pom.xml` 中腾讯云 Java SDK（如 `tencentcloud-sdk-java-trtc` 及其传递的 `common`）使用 **Maven Central 固定版本号**，禁止使用 `LATEST`。
- **任务生命周期**：停止对话、控制对话等接口**本期不在范围**（仅「开始」）。
- **密钥与联调**：SecretId/SecretKey、UserSig 密钥、LLM API Key 等仅通过**环境变量**（或本地未提交的 profile）注入，保证克隆仓库即可配置后本地运行。

## Capabilities

### New Capabilities

- `trtc-start-ai-conversation`: 覆盖 TRTC StartAIConversation 的 HTTP 入口、Header 鉴权与客户端 UserId 解析、机器人 UserId/UserSig、固定地域与 SDK 版本、LLM/TTS 配置加载与日志红线、错误与配置缺失时的 HTTP 语义。

### Modified Capabilities

- （无）现有 `api-routing-policy` 已要求 `con.aidenjia.digitalhuman.conversation.api` 下控制器显式映射 `/api/v1`；新接口沿用同一约定即可，不改变既有规范条文。

## Impact

- **代码**：新增 `api` 包下 REST 控制器、TRTC 调用服务、可选配置属性类、Servlet Filter 或等效机制（仅保护 `/api/v1/trtc/**` 或具体路径，不影响 `/api/v1/echo` 等既有接口）。
- **配置**：`application.properties` / 环境变量增加 `trtc.*`、`api.security.*`（或等价前缀）；可选 `.env.example` 仅列键名。
- **依赖**：腾讯云 TRTC Java SDK + 现有 `tls-sig-api-v2`（UserSig）；SDK 版本锁定。
- **测试**：MockMvc / 集成测试覆盖鉴权失败、校验失败；对真实腾讯云调用可通过 MockBean 或契约测试隔离 CI。
- **运维**：部署需注入 TRTC 与 LLM 相关环境变量；日志审查确保无 LLMConfig 全量输出。
