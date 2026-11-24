## ADDED Requirements

### Requirement: Upload user scene image via server proxy

系统 SHALL 提供 `POST /api/user-scene-images`（JWT Bearer，`multipart/form-data`）。系统 MUST 将 `user_id` 设为 JWT `sub`，MUST NOT 信任表单中的 userId。请求 MUST 包含文件字段 `file`、非空 `name`（trim 后长度 1–50）、以及 `scope`（`companion` 或 `dimension`）。系统 MUST NOT 要求或接受 `companionBusinessCode` / `companionId`（图库不绑角色）。

系统 MUST 仅接受 `image/jpeg`、`image/png`、`image/webp`；字节大小 MUST 不超过配置上限（缺省 10MiB）。校验失败 MUST 返回 400 且 MUST NOT 写库、MUST NOT 保留 COS 对象。

上传成功后系统 MUST 将对象写入腾讯云 COS，再插入 `user_scene_image` 行（应用侧雪花 `id`）。`object_key` MUST 形如 `{env}/scene/{userId}/{scope}/{id}.{ext}`（`env` 为 `local`/`dev`/`test`/`prod`，与 Spring profile 对齐）。`url` MUST 为 COS 私有 GET 签名 URL（TTL 1 天，`ScenePreferenceDefaults.SIGNED_URL_EXPIRE_SECONDS`），**上传时签发并落库**。COS 上传失败 MUST 返回 5xx 或项目上游错误信封且 MUST NOT 插入行。成功响应 `data` MUST 包含 `id`、`name`、`url`、`scope`。

#### Scenario: Successful upload returns id name and url

- **WHEN** 已鉴权用户提交合法 `file`、`name` 与 `scope=companion`
- **THEN** 系统 MUST 写入 COS 与 `user_scene_image` 行，并返回 200 且 `data` 含非空 `id`、`name`、`url`

#### Scenario: Missing name fails

- **WHEN** 未传 `name` 或 trim 后为空
- **THEN** 系统 MUST 返回 400 且 MUST NOT 写库

#### Scenario: Invalid scope fails

- **WHEN** `scope` 不是 `companion` 或 `dimension`
- **THEN** 系统 MUST 返回 400 且 MUST NOT 写库

#### Scenario: Disallowed content type fails

- **WHEN** 文件 Content-Type 不在 jpeg/png/webp 内
- **THEN** 系统 MUST 返回 400 且 MUST NOT 写库

---

### Requirement: List user scene images with used flag

系统 SHALL 提供 `GET /api/user-scene-images?scope=`（JWT Bearer）。`scope` MUST 为 `companion` 或 `dimension`；缺省或非法 MUST 返回 400。系统 MUST 只返回当前用户该 `scope` 下的行，按 `created_at` 降序。每项 MUST 包含 `id`、`name`、`url`、`used`。

返回的 `url` MUST 为可 GET 的 COS 签名 URL。系统 MUST 优先使用库内 `url`；若解析 `q-sign-time` 判定已过期或距失效不足 `SIGNED_URL_RENEW_SKEW_SECONDS`（5 分钟），MUST 重新签发签名 URL 并回写 `user_scene_image.url`。

`used=true` MUST 当且仅当：该用户使用中配置的 `scene_source='cloud'` 且 `selected_scene_image_id` 等于该项 `id`。陪伴「使用中」MUST 为该用户 `enabled=true` 且 `updated_at` 最新的 `user_companion_config`；次元 MUST 为该用户唯一 `user_dimension_config`。当前为 local（`scene_source=''`）或无配置时，全部 `used` MUST 为 `false`。

#### Scenario: List returns only current user and scope

- **WHEN** 用户 A 在 `scope=companion` 下有图，用户 B 亦有图
- **THEN** 用户 A 的 GET MUST NOT 返回用户 B 的行

#### Scenario: Cloud selected image is used

- **WHEN** 使用中配置 `scene_source='cloud'` 且选用 id 等于列表某项
- **THEN** 该项 `used` MUST 为 true，其余 MUST 为 false

#### Scenario: Local scene marks all unused

- **WHEN** 使用中配置为 local（`scene_source=''`）
- **THEN** 列表每项 `used` MUST 为 false

#### Scenario: Missing scope fails

- **WHEN** GET 未传 `scope`
- **THEN** 系统 MUST 返回 400

---

### Requirement: Hard-delete scene image and revert configs to local

系统 SHALL 提供 `DELETE /api/user-scene-images/{id}`（JWT Bearer）。系统 MUST 仅删除当前用户拥有的行；他人 id MUST 返回 404 或 400 且 MUST NOT 删 COS。系统 MUST 在同一事务内：将引用该 id 的 `user_companion_config` 与 `user_dimension_config` 行设为 `scene_source=''` 且 `selected_scene_image_id=NULL`，再删除 `user_scene_image` 行，然后删除 COS 对象。COS 删除失败 MUST NOT 回滚已提交的库删除；系统 MUST 打 WARN。成功 MUST 返回统一成功信封。

#### Scenario: Owner deletes image

- **WHEN** 已鉴权用户删除自己的场景图
- **THEN** 系统 MUST 删除该行；再次 GET 列表 MUST 不含该 id

#### Scenario: Delete selected image reverts config to local

- **WHEN** 删除的图正被某配置 `selected_scene_image_id` 引用
- **THEN** 那些配置行 MUST 变为 `scene_source=''` 且 `selected_scene_image_id` 为 null

#### Scenario: Other users image is rejected

- **WHEN** 用户删除不属于自己的 `id`
- **THEN** 系统 MUST 拒绝且 MUST NOT 删除该行或 COS 对象

---

### Requirement: Scene image APIs require JWT

系统 MUST 对 `/api/user-scene-images` 与 `/api/user-scene-images/**` 要求有效 Users JWT。

#### Scenario: Unauthenticated upload is rejected

- **WHEN** 未携带有效 JWT 调用 POST `/api/user-scene-images`
- **THEN** 系统 MUST 拒绝请求（401 或项目等价）
