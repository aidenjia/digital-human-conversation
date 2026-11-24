## Context

腾讯云 TRTC 中：

- **云 API**（`trtc.tencentcloudapi.com`）使用 **CAM** 的 `SecretId` / `SecretKey`（`TRTC_CLOUD_SECRET_ID` / `TRTC_CLOUD_SECRET_KEY`）。
- **UserSig**（`TLSSigAPIv2`）使用 **TRTC 控制台** 的 SdkAppId 与 **应用密钥**（`TRTC_USERSIG_SDK_APP_ID` / `TRTC_USERSIG_SECRET_KEY`），与 CAM 密钥通常 **不是** 同一字符串。

`StartAIConversation` 请求体中的应用标识仍使用 `TRTC_SDK_APP_ID`（`trtc.sdk-app-id`），与 UserSig 所用 SdkAppId 允许不同。

## Goals / Non-Goals

**Goals:**

- 配置与校验显式区分：`trtc.cloud.*` vs `trtc.usersig.*`。
- `.env` 与文档列出两套变量，避免误用 CAM SecretKey 生成 UserSig。

**Non-Goals:**

- 不在本变更中重新引入「UserSig 过期时间」的环境变量配置（仍由 `application.properties` 固定）。

## Decisions

1. **UserSig 凭据**  
   - `TLSSigAPIv2(trtc.usersig.sdk-app-id, trtc.usersig.secret-key)`，来源 `TRTC_USERSIG_*`。

2. **云 API 凭据**  
   - `Credential(cloud.secretId, cloud.secretKey)`，来源 `TRTC_CLOUD_*`。

3. **请求 SdkAppId**  
   - `StartAIConversationRequest#setSdkAppId` 使用 `trtc.sdk-app-id`（`TRTC_SDK_APP_ID`）。

4. **校验**  
   - `validateConfiguration()` 同时校验云 API 四项与 UserSig 的 SdkAppId、secretKey。

## Risks / Trade-offs

- `.env` 键名略多，但符合腾讯云「两套密钥」的真实模型，避免运行时签名错误。
