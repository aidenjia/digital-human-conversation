## Design Summary

为数字人陪伴提供**定制音色生产**能力：用户填表建单 → 拉取腾讯正式朗读文案 → 录音检测 → 创建训练 → 轮询落库；成功后继续走已有音色列表 / 用户配置 upsert / join 解析。

本期范围锁定：

- **不做支付**（建单直接 `pay_status=2`，满足库 CHECK）
- **用户资产模型**（`companion_custom_voice.user_id`；不绑 `companion_id`）
- **一句话复刻（TaskType=5）**（短录音；`Domain`/`TextLanguage` 生效；`provider_voice_ref` 存可 parse 的数字 `VoiceType`，现有 join `Integer voice_type` 可用）
- **FastVoiceType 进通话**本期不做（Gate 未过；Describe 返回的 FastVoiceType 忽略）
- 库表已具备生产字段；已新增 `training_text_id`、`provider_task_id`（本 change 不负责跨环境 DDL 脚本强制，但实现须映射这两列）

部署：K8s `replicas: 2`；Postgres + Redis 已有。AudioId 等短生命周期态用 Redis（可选；同请求 Detect→Create 可先不缓存），禁止仅 JVM 本地缓存跨请求态。

## Alternatives Considered

### Alternative A: 按桌面旧方案恢复 companion 绑定
- **Approach**: `companion_custom_voice.companion_id` UNIQUE；定制时清空 `companion.voice_id`。
- **Pros**: 与桌面《声音复刻》字面一致。
- **Cons**: 与现库/配置页冲突（`companion_id` 已删；音色挂在 `user_companion_config`）。
- **Why not chosen**: 活规格与代码已是用户资产模型。

### Alternative B: 零加列，TextId/TaskId 只放 Redis
- **Approach**: 不落库腾讯单号，全靠 Redis TTL。
- **Pros**: 无 DDL。
- **Cons**: 过期/丢 key 无法续查；与双副本运维不友好。
- **Why not chosen**: 已加列；异步生产必须可续查。

### Alternative C: 腾讯「基础版」长音频复刻
- **Approach**: 非 TaskType=5；长语料路径；不依赖 Domain/TextLanguage 一句话参数。
- **Pros**: 与部分旧桌面文档字面「基础版」接近。
- **Cons**: 与产品一句话短录音、场景/语言下拉、现行代码 `TaskType=5` 不一致。
- **Why not chosen**: 产品从一开始钉死一句话版。

### Alternative D: 一句话 + FastVoiceType 进 join
- **Approach**: TaskType=5，产物字符串 FastVoiceType，扩展 ConvoAI join。
- **Pros**: 对齐腾讯一句话主产物之一。
- **Cons**: join 无 `fast_voice_type`；声网 Gate 未过。
- **Why not chosen**: 一期落库数字 `VoiceType` 即可进现有 TTS；FastVoiceType 后置。

## Agreed Approach

在 conversation 新增定制音色**生产** API（薄 Controller + `customvoice` 编排包 + 腾讯 VRS Client），写回 `companion_custom_voice`；**选用与进房**复用现有 companion catalog / user-companion-config / Convoai join。

默认产品决策（实现默认值）：

1. **一句话复刻 TaskType=5**；终态 `provider_voice_ref` = 数字 `VoiceType`（忽略 FastVoiceType）
2. 看样例：`GET /training-text` 拉腾讯文案，不建单；提交时客户端回传同一 `textId`
3. 语料：**上传走后端**（前端交文件给 conversation → 服务 PutObject 到 COS）；`corpus_uri` 存路径；检测用上传字节或再读 COS
4. VRS Client：**仅真实实现**（禁止 Mock）
5. **不做**前端直传临时凭证 / 浏览器 CORS 直传
6. 试听 TTS 接口后置
7. 语言/领域：前端下拉 + 库 text code；Java 枚举校验；服务端映射腾讯 Domain/VoiceLanguage

## Key Decisions

1. `user_id` = JWT `sub`；禁止跨用户操作生产单。
2. 建单 INSERT：`pay_status=2`，`status=0`；`training_text_id`/`provider_task_id` 默认空串。
3. **产品对齐流程**：看样例（只拉腾讯文案，不建单）→ 录音上传 → 提交（建单+检测+训练）→ 轮询。
4. API：`GET .../training-text`、语料上传（后端收文件）、`POST .../custom-voices`、`GET .../{businessCode}`；对外生产单键 `custom_00N`。
5. 提交时写入 `training_text_id`、`corpus_uri`；训练中写 `provider_task_id`；终态写 `provider_voice_ref` / `fail_reason`。
6. `source_language` 合法值：`zh-CN` / `en-US` / `ja-JP`；`domain`：`companion` / `customer_service` / `marketing` / `news`。
7. 包归属：新建业务包（勿塞进 `trtc`）；infrastructure 放腾讯 VRS + COS；密钥走 Properties + 环境变量。
8. 新接口须 JWT `authenticated()`；统一 `ApiResponse` 信封。
9. Non-goals：支付、试听、FastVoiceType Gate、恢复 `companion_id`、枚举表、`dimension_*` 表、腾讯基础版长音频、**前端直传 COS**。
10. COS：后端上传对象；私有桶；对象键 `{envPrefix}/custom-voice/{userId}/{uuid}.ext`（联调 envPrefix=`test`，与桶内 local/dev 并列）；检测用上传字节或再读 COS；**本期含语料临时读 URL（预签名，短 TTL）**。

## Open Questions

- （已决）复刻类型 = 一句话 `TaskType=5`；落库数字 `VoiceType`。（UI 长音频文案以产品一句话为准，前端需改提示）
- （已决）对外 `businessCode=custom_00N`；技术 `id` 仅内部。
- （已决）语料 **上传走后端**（非前端直传）；COS 存文件；`corpus_uri` 落库。
- （已决）本服务提供 COS 上传临时凭证 → **作废**；改为后端收文件后 PutObject。
- （实现时核实）`DescribeVRSTaskStatus` 的 Status 映射；具体桶名/地域由环境变量配置；上传 API 是独立接口还是并进 submit（multipart）。
