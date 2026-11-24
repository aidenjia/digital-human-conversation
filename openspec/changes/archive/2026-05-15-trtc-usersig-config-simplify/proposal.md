## Why（当前结论）

此前曾尝试将 UserSig 与云 API 合并为同一套 `TRTC_SDK_APP_ID` / `TRTC_CLOUD_SECRET_KEY`。实际部署中 **TRTC 控制台「应用密钥」** 与 **CAM 云 API 密钥** 常为两套不同凭据；合并会导致联调/生产配置错误。

## What Changes（回退后行为）

- **恢复** 环境变量 `TRTC_USERSIG_SDK_APP_ID`、`TRTC_USERSIG_SECRET_KEY` 及绑定 `trtc.usersig.sdk-app-id`、`trtc.usersig.secret-key`。
- `TLSSigAPIv2` 仅使用 **UserSig 专用** SdkAppId + 应用密钥；`Credential` / `StartAIConversationRequest` 仍使用 `TRTC_SDK_APP_ID` 与 `TRTC_CLOUD_SECRET_*`。
- `trtc.usersig.expire-seconds` 仍为仓库内固定值（1 天），不要求写入 `.env`。

## Capabilities

### Modified Capabilities

- `trtc-start-ai-conversation`：规格与实现要求 **UserSig 与云 API 密钥分离** 注入。

## Impact

- `application.properties`、`TrtcProperties`、`TrtcAiConversationService`
- `.env.*`、`StartConversation.md`、`application-test.properties`
- OpenSpec 本目录下 `spec.md` 已与上述行为对齐
