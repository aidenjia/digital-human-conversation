## 1. 依赖与配置骨架



- [x] 1.1 在 `pom.xml` 将 `tencentcloud-sdk-java-trtc` 固定为 `3.1.1289`（移除 `LATEST`；依赖 `common` 由传递或显式同版本对齐），`mvn -q verify` 通过

- [x] 1.2 在 `application.properties` 增加 `trtc.robot.user-id`、`api.security`（Header 名、固定 token 绑定环境变量）、LLM/TTS 资源路径等占位，敏感项仅 `${ENV:}` 形式

- [x] 1.3 新增 `classpath:trtc/llm-config.json` 与 `classpath:trtc/tts-config.json` 模板（不含真实密钥），并在注释或 `StartConversation.md` 中说明字段含义



## 2. 鉴权与请求解析



- [x] 2.1 实现仅匹配 `/api/v1/trtc/**` 的 Filter（或等价 WebMvc 注册），读取可配置 Header，校验 `<userId>|<secret>` 格式并将 `userId` 放入 request attribute

- [x] 2.2 固定 token 未配置时 TRTC 路由返回 401（或设计文档选定的一致行为），并单测覆盖

- [x] 2.3 确认 `POST /api/v1/echo` 在无 TRTC Header 时行为不变（回归 `WebApiFoundationIT`）



## 3. TRTC 调用与领域服务



- [x] 3.1 新增 `@ConfigurationProperties` 绑定 `trtc.*` / `api.security.*`，校验 `SdkAppId`、云密钥、robot user id 等缺失时在服务层抛出一致的业务异常

- [x] 3.2 使用 `TLSSigAPIv2` 为机器人 `UserId` 生成 `UserSig`（过期时间可配置），组装 `AgentConfig`（含 `TargetUserId`）

- [x] 3.3 实现加载 LLM/TTS JSON、合并 `APIKey`/`APIUrl`（自环境），构建 `StartAIConversationRequest`（地域 `ap-guangzhou`），调用 `TrtcClient.StartAIConversation`

- [x] 3.4 映射 `TencentCloudSDKException` 与配置缺失为 Problem Details（401/503/502 等与 `design.md` 一致），禁止向客户端暴露内部堆栈



## 4. HTTP 层与可观测性



- [x] 4.1 在 `api` 包新增 `POST /api/v1/trtc/ai-conversations/start` 控制器与校验过的请求 DTO（roomId、可选 roomIdType、sessionId）

- [x] 4.2 成功/失败日志仅输出白名单字段；代码检索确认无 `LLMConfig` / `UserSig` / API Key 的整串输出

- [x] 4.3 （可选）`.env.example` 列出所需环境变量键名，无默认值



## 5. 测试与文档



- [x] 5.1 MockMvc：`@MockBean` 服务层或 HTTP 层测试鉴权失败、body 校验失败、成功路径 JSON 形状

- [x] 5.2 `application-test.properties`（或 `@DynamicPropertySource`）为测试注入最小 `api.security.fixed-token` 等，保证 `contextLoads` 与现有 IT 通过

- [x] 5.3 更新 `StartConversation.md` 或 README 片段：Header 格式、环境变量清单、与腾讯云文档链接

