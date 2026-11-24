# digital-human-conversation

数字人实时会话演示，包含 Java 21 / Spring Boot 3.5 后端和 React / Vite 前端。百炼 WebRTC 支持语音或文字输入、语音与字幕回复、打断和 VRM 数字人舞台。声网 ConvoAI 支持语音通话和 RTC 进房。

## 快速开始

需要 JDK 21、Maven、Node.js 24 和 npm。以下命令在 PowerShell 中运行。

在仓库根目录启动后端。百炼会话需要你自己的 DashScope API Key。若 Key 归属指定业务空间，设置 `BAILIAN_WORKSPACE_ID`。

```powershell
$env:DASHSCOPE_API_KEY = '<你的 DashScope API Key>'
$env:API_SECURITY_DEMO_TOKEN_ENABLED = 'true'
$env:API_SECURITY_DEMO_TOKEN = [guid]::NewGuid().ToString('N')
$env:API_SECURITY_JWK_SET_URI = 'https://issuer.example.test/.well-known/jwks.json'
$env:SERVER_ADDRESS = '127.0.0.1'
Write-Host "本地演示登录 Token：$env:API_SECURITY_DEMO_TOKEN"
mvn spring-boot:run
```

示例 JWKS 地址用于本地固定 Token 演示。接入真实 JWT 时，配置实际的 issuer 和 JWKS 地址，并关闭演示开关。Spring Boot 从进程环境读取配置。也可以使用仓库根目录下由 Git 忽略的 `config/application-local.yml`。

在另一个终端从仓库根目录启动前端。

```powershell
Set-Location conversation-ui
npm ci
npm run dev
```

打开前端终端显示的地址（通常为 `http://localhost:5173`），输入后端终端显示的演示 Token，选择「百炼 WebRTC」。语音模式需要麦克风权限。默认 VRM 需要自行准备，安装位置和导入方式见 [Avatar 说明](conversation-ui/AVATAR.md)和 [模型来源](conversation-ui/public/models/README.md)。使用声网入口前按下方说明配置声网凭据和下游 LLM/TTS。

## 接口

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| POST | `/api/convoai/agents` | 创建声网 AI Agent，返回进房凭据 |
| DELETE | `/api/convoai/agents` | 校验 Agent 归属并结束会话 |
| POST | `/api/rtc/credentials` | 单独签发声网进房凭据 |
| POST | `/api/bailian/realtime/webrtc` | 百炼 SDP 交换，签发一次性控制票据 |
| WS | `/ws/bailian/realtime-events` | 百炼 WebRTC 控制事件 |
| GET | `/` | 应用标识与版本 |
| GET | `/actuator/health` | 健康检查 |
| GET | `/actuator/prometheus` | 监控指标 |

`/api/**` 使用 `Authorization: Bearer <JWT>`，JWT 本地校验签名、issuer 与有效期，`sub` 是会话用户。成功响应使用 `ApiResponse`：`code=200`、`success=true`、业务数据在 `data`；失败保持统一信封。请求可带 `X-Api-Culture: zh|en`；现有业务接口使用隐式 v1。

本地演示旁路受 `api.security.demo-token-enabled` 控制，仅允许上述会话 HTTP 接口与 WebRTC 控制握手，生产默认关闭。不要把永久 API Key、Bearer 或 RTC token 放入日志。

## 声网 ConvoAI

`POST /api/convoai/agents`：

```json
{"deviceId":"browser-1","digitalHumanType":"UE5","timezone":"Asia/Shanghai"}
```

`digitalHumanType` 支持 `UE5`、`Unity`、`MMD`，继续传给下游 Agent，三种类型统一使用服务端配置的 TTS、欢迎语及默认语言，不读取用户保存配置。`timezone` 可省略。

返回 `data` 包含 `appId/channel/token/userId/agentId`。浏览器用声网 SDK 进房、发布麦克风并订阅 AI 远端音频。结束时调用：

```json
{"agentId":"<join 返回的 agentId>"}
```

同用户 join 进行中通过进程内互斥防止重复创建，竞争失败立即返回 409；请求成功或失败都会释放占用。成功后可同步调用下游 Agent session warm，warm 失败不改变 join 成功结果。leave 必须匹配内存中保存的当前用户 Agent 归属，未知、过期或越权返回 404。

声网 TTS 仍使用 Tencent vendor，需要 `CONVOAI_TTS_APP_ID` 及 `TENCENT_SECRET_ID/TENCENT_SECRET_KEY`。这些参数用于声网会话的语音合成配置。

## 百炼 WebRTC

浏览器直连百炼传输音频，服务端代发 SDP、编排控制事件。完整字段与事件说明见 [前端对接文档](docs/frontend-realtime-voice-feishu.md)。

1. 浏览器生成 Offer，以 `{"offerSdp":"...","model":"..."}` POST 到信令接口。
2. 返回 `answerSdp/relayId/model`；`relayId` 有效期 60 秒，绑定用户与模型，一次性使用。
3. 连接 `/ws/bailian/realtime-events?access_token=<JWT>&relayId=<票据>&persona=<人设>&voice=<百炼音色>&enableAction=true`。
4. 控制连接成功后设置远端 SDP。音频通过 PeerConnection RTP 播放；数据通道事件转发到控制连接，`downstream.command.event` 写回百炼数据通道。
5. 结束时关闭控制连接、PeerConnection 和媒体资源；收到 `server.draining` 后关闭并重新创建会话。

允许模型为 `qwen-audio-3.0-realtime-flash` 和 `qwen-audio-3.0-realtime-plus`，省略模型使用 flash。人设与百炼音色名称在本次会话传入，不提供音色目录、复制或试听接口。固定欢迎语、字幕、文本输入、打断、动作回执均通过 WebRTC 控制链路处理。浏览器 WebRTC 页面不需要声网进房凭据。

## 服务端配置

默认 profile 为 `local`。凭据按所用链路配置，生产环境设置 `SPRING_PROFILES_ACTIVE=prod`，并配置 OpenID Connect 身份提供方。

| 配置 | 用途 |
| --- | --- |
| `DASHSCOPE_API_KEY`、`BAILIAN_WORKSPACE_ID` | 百炼服务端信令凭证 |
| `CONVOAI_APP_ID`、`CONVOAI_USERNAME`、`CONVOAI_PASSWORD` | 声网 ConvoAI |
| `CONVOAI_RTC_APP_CERTIFICATE` | RTC token 签发 |
| `CONVOAI_LLM_URL`、`CONVOAI_LLM_API_KEY` | 下游 Agent LLM 回调 |
| `CONVOAI_SESSION_WARM_ENABLED`、`CONVOAI_SESSION_WARM_URL`、`CONVOAI_SESSION_WARM_TIMEOUT_MS` | 会话预热 |
| `CONVOAI_TTS_APP_ID`、`TENCENT_SECRET_ID`、`TENCENT_SECRET_KEY` | 声网 Tencent TTS |
| `API_SECURITY_ISSUER_URI`、`API_SECURITY_JWK_SET_URI` | OpenID Connect JWT 校验 |
| `API_SECURITY_DEMO_TOKEN_ENABLED`、`API_SECURITY_DEMO_TOKEN` | 本地演示鉴权 |
| `AVATAR_DECISION_API_KEY`、`AVATAR_DECISION_BASE_URL`、`AVATAR_DECISION_MODEL`、`AVATAR_DECISION_TIMEOUT_MS` | WebRTC Avatar 动作决策 |

服务不需要 Redis、关系数据库或对象存储，只支持 **1 个后端进程 / 1 个副本** 的单机演示。声网 Agent 归属保存在内存，TTL 默认 86400 秒（`CONVOAI_AGENT_OWNERSHIP_TTL_SECONDS`）；WebRTC 票据在 60 秒后失效，按用户原子消费一次，未使用的过期条目定期清理。

进程重启会丢失归属和票据，需要重新建立会话；旧 Agent 的 leave 会返回 404。重启不会自动销毁云端 Agent，需依赖声网空闲退出（当前默认 300 秒）或另行处理。不要部署多副本、多个 JVM 或多 worker，也不要做滚动更新时的新旧实例重叠；演示部署使用单副本 `Recreate`。声网、百炼、Agent 和登录服务仍需联网与相应凭据，不是离线部署。

## 浏览器与测试

Java 包以 `con.aidenjia.digitalhuman.conversation` 为根。Windows 将 `CON` 视为保留名称，因此源码目录使用 `src/main/java/aidenjia/` 和 `src/test/java/aidenjia/`，Maven 按 Java 文件中的包声明编译。

```powershell
Set-Location conversation-ui
node --test tests/*.test.ts
npm run lint
npm run build
```

部分测试读取默认 VRM 的实际骨骼，需要先准备 [模型文件](conversation-ui/public/models/README.md)。前端 API 地址及部署配置见 [前端 README](conversation-ui/README.md)。

后端验证：

```powershell
mvn -B -ntp clean verify
```

保留的测试覆盖会话接口、鉴权、统一信封、WebRTC 控制状态、打断及 Avatar 回执。需要真实模型/API 的探针默认跳过，不能用单元测试结果替代真实通话验收。

## 部署

百炼 WebRTC 信令失败时，WARN 会记录 `httpStatus/upstreamCode/reason/requestId/workspaceId/model`。其中 `reason` 仅为从错误码及消息提取的诊断分类，不代替百炼原始错误结论；不打印完整响应正文、API Key 或 SDP。403 应结合具体错误码检查当前 Key 对应的业务空间、模型权限或账户状态，而不是调整页面登录 Token。

Docker 运行 Java 21，监听 8080；前端静态文件单独构建部署。反向代理需要支持控制连接的 WebSocket Upgrade，TLS 与麦克风权限要求仍适用。优雅下线见 [运行说明](docs/k8s-graceful-shutdown-requirements.md)。

后端打包后可直接运行 `java -jar target/digital-human-conversation-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod`。协议和行为约定见 [OpenSpec](openspec/specs/README.md)。
