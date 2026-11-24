## 1. API 模型

- [x] 1.1 新增 `ApiResponse<T>` 与 `StartConversationData` DTO
- [x] 1.2 `TrtcAiConversationService` 返回 `StartConversationResult`（含客户端 UserSig）
- [x] 1.3 控制器返回 `ApiResponse.ok(data)`

## 2. 测试与文档

- [x] 2.1 更新 `TrtcAiConversationIT` 断言信封与 data 字段
- [x] 2.2 更新 `StartConversation.md` 成功响应示例
