## Context

本服务是 Agora ConvoAI HTTP 网关（Spring Boot 3.5、replicas=2、ClusterIP、无粘性）。产品要切到百炼 `qwen-audio-3.0-realtime-flash` WebSocket 端到端语音，同时用声网 RTC 只做 3A。WebSocket 中继会把通话状态绑在单 Pod 上，与现有部署与关停约定冲突；用户选定 **临时 Key + 客户端直连**。

## Goals / Non-Goals

**Goals:**
- 新增 RTC 凭据接口（无 agent / 无 LLM/TTS/VAD）
- 新增百炼临时 Key 接口（永久 Key 仅服务端；响应含建连元数据）
- 按 `digitalHumanType` 注入陪伴/次元人设到建议 `instructions`；音色用配置默认千问系统音色
- 统一 `ApiResponse` + Bearer；密钥与临时 Key 脱敏日志
- 文档写清客户端：3A、直连 WS、探活、续期

**Non-Goals:**
- 不实现本服务到百炼的 WebSocket 中继
- 不修改 / 不替换现有 ConvoAI join/leave
- 不映射腾讯 TTS `voice_type` → 千问 `voice` / 声音复刻
- 不在服务端做 3A、不存 PCM、不引入新中间件
- 不实现 UE5/Unity 客户端代码

## Decisions

1. **路径隔离**：`/api/rtc/credentials` 与 `/api/bailian/realtime-tokens`，不挂在 `/api/convoai/**`，避免与 agent 生命周期混淆。
2. **RTC**：复用 `AgoraRtcTokenService.buildUserToken` + `RoomIdGenerator.generate()`；`userId=JWT sub`；无 ownership Redis。
3. **临时 Key**：服务端 `POST https://dashscope.aliyuncs.com/api/v1/tokens?expire_in_seconds={ttl}`，`Authorization: Bearer ${DASHSCOPE_API_KEY}`；默认 TTL 1800；失败映射为上游/配置类错误（对齐现有 `UpstreamException` / `ConfigurationException` 模式）。
4. **wsUrl**：由配置 `workspace-id` + 固定模型拼北京域业务空间域名；禁止把永久 Key 放进 URL。
5. **人设**：UE5→陪伴 catalog `instruction`；MMD→次元 `personaText`；Unity→yml 默认 instructions。无配置时用 yml 默认，不失败。
6. **音色**：`bailian.realtime.default-voice`（如 `longanqian`）；本期不读腾讯音色表。
7. **欢迎语**：若用户配置开启且文案非空，在 token 响应带 `greetingText`；是否发送由客户端决定。
8. **分层**：Controller 薄；OkHttp 客户端在 `infrastructure.bailian`；禁止 api 包直调上游。

## Risks / Trade-offs

- [临时 Key 权限过宽] → 缩短 TTL、仅下发成功响应、禁止日志；后续可换更窄权限 Key（若百炼支持）
- [客户端假死] → README 强制 Ping/Pong + 应用看门狗；`server_error` 必须重建会话
- [TTL 到期] → 客户端在 `expiresAt` 前刷新；服务端不保证会话续期
- [人设与腾讯音色割裂] → 文档标明 Non-goal；产品后续单独立项声音复刻
- [百炼 tokens 不可用] → 503/上游错误；不回退永久 Key 给客户端

## Migration Plan

1. 配置注入 `DASHSCOPE_API_KEY`、`BAILIAN_WORKSPACE_ID`、默认 voice/TTL（local `.env.local`）
2. 发布服务（无破坏性 API）
3. 客户端切新双端点并联调；ConvoAI 旧客户端不受影响
4. 回滚：关掉新端点路由或清空 Key（返回配置错误）；旧 ConvoAI 路径不变

## Open Questions

- 无阻塞项。OpenSpec apply 前若产品要求「token 响应不带 instructions」，再收窄响应字段即可。
