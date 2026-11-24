## 1. 配置与属性绑定

- [x] 1.1 `application.properties` 恢复 `trtc.usersig.sdk-app-id`、`trtc.usersig.secret-key`（`${TRTC_USERSIG_SDK_APP_ID:}` / `${TRTC_USERSIG_SECRET_KEY:}`），注释说明与 CAM 密钥区别
- [x] 1.2 `TrtcProperties.UserSig` 恢复 `sdkAppId`、`secretKey` 与 `expireSeconds`

## 2. 服务与校验

- [x] 2.1 `TrtcAiConversationService`：`TLSSigAPIv2` 使用 UserSig 专用 id/密钥；请求体 `setSdkAppId` 仍用 `trtc.sdk-app-id`
- [x] 2.2 `validateConfiguration()` 校验 `userSig.sdkAppId`、`userSig.secretKey`

## 3. 环境与文档

- [x] 3.1 `.env.local`、`.env.test`、`.env.prod` 增加 `TRTC_USERSIG_SDK_APP_ID`、`TRTC_USERSIG_SECRET_KEY`
- [x] 3.2 `StartConversation.md` 配置表区分两套凭据
- [x] 3.3 `src/test/resources/application-test.properties` 增加 `trtc.usersig.*` mock 值

## 4. 验证

- [x] 4.1 `mvn clean test` 通过
