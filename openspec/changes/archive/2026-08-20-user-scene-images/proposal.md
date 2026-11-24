## Why

陪伴与次元配置页需要用户上传场景背景图、浏览已上传列表、并在启动时指定当前场景（含客户端本地场景）。现有配置只覆盖音色/微调/互动，没有对象存储与场景资产模型，无法支撑该产品路径。

## What Changes

**用户场景图资产**
- From: 无场景表、无 COS、无上传/列表/删除 API
- To: `user_scene_image`（`user_id` + `scope`，不绑角色；必填 `name`）+ `POST/GET/DELETE /api/user-scene-images`；本服务代理上传腾讯云 COS；落库稳定公有/CDN `url`
- Reason: 图库是用户资产，角色只决定选用哪张
- Impact: 非破坏性新接口；新增 COS 依赖与环境变量

**启动保存必选场景**
- From: 陪伴/次元 upsert 无场景字段
- To: 必选 `sceneSource=local` 或 `cloud` + `sceneImageId`；local 落库 `scene_source=''` 且选用 id 为空
- Reason: 对齐音色必选；不允许「无场景」
- Impact: **破坏性**（旧客户端未传场景 → 400）

**硬删与回退**
- From: 无
- To: 删除行 + COS 对象；若被 config 引用则那些行回退 local
- Reason: 避免悬空选用
- Impact: 删除当前图后客户端改用本地场景

## Capabilities

### New Capabilities

- `user-scene-image`: 用户场景图上传、列表（含 `used`）、硬删；COS 元数据与归属校验

### Modified Capabilities

- `user-companion-config`: POST 必选场景（local / cloud id）；config 增加 `scene_source` 与 `selected_scene_image_id`
- `user-dimension-config`: 同上（dimension scope 图库）
- `openid-jwt-validation`: 受保护路径增加 `/api/user-scene-images`/`**`

## Impact

- 代码：`api.scene`、`scene`、`infrastructure.cos`、persistence、陪伴/次元 upsert、`ResourceServerSecurityConfig`、鉴权与业务测试
- 文档：`README.md`、`.cursor/rules/java-api-convoai.mdc`、DDL 说明
- DB：新建 `user_scene_image`；两张 config 加列（无 FK）
- 依赖：腾讯云 COS Java SDK（固定版本）；CD 环境变量（bucket/region/密钥）
- Redis / join：不把场景 URL 注入 ConvoAI；改选用时仍按现有逻辑刷新 companion/dimension 缓存
- 客户端：新上传列表删除；upsert 必带场景选型
