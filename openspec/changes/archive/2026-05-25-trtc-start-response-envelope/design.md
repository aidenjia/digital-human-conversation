## Context

`POST /api/v1/trtc/ai-conversations/start` 已实现鉴权、StartAIConversation 调用，成功时返回 `taskId`/`requestId`。产品要求改为统一 `ResponseBody<T>` 信封，并在 `data` 中返回客户端 TRTC 进房字段。

## Goals / Non-Goals

**Goals:**

- 成功响应：`{ code: 0, message, data: { sdkAppId, userId, userSig, strRoomId } }`
- 为鉴权解析出的客户端 `userId` 生成 UserSig（`TRTC_USERSIG_*` 密钥）
- `strRoomId` 与请求体 `roomId` 一致
- 保留 StartAIConversation 调用与 task 日志

**Non-Goals:**

- 错误响应改为信封（仍用 Problem Details + HTTP 状态码）
- 在响应中返回 `taskId`/`requestId`
- 停止/控制对话等生命周期 API

## Decisions

1. **通用类型 `ApiResponse<T>`**（`api` 或 `web` 包）：`code`、`message`、`data`；工厂方法 `ok(data)` 设置 `code=0`、`message="success"`。
2. **`sdkAppId`**：使用 `trtc.usersig.sdk-app-id`（与客户端 UserSig 一致），JSON 序列化为 number。
3. **客户端 UserSig**：在 `TrtcAiConversationService` 内与机器人 UserSig 同样调用 `TLSSigAPIv2#genUserSig`，目标为 `clientUserId`。
4. **服务返回值**：`startConversation` 返回领域结果 `StartConversationResult`（含进房字段），不再向控制器暴露 `StartAIConversationResponse`。
5. **日志**：继续记录 `taskId`/`requestId`；禁止在日志中输出完整 `userSig`。

## Risks / Trade-offs

- **[BREAKING]** 已有集成方依赖 `taskId` → 文档与版本说明；任务 ID 仅服务端可见。
- **双 UserSig 生成** → 可接受，均为本地 HMAC，无额外云 API。
