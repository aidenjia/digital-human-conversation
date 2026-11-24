## 1. Persistence and identity

- [x] 1.1 新增 `Companion` / `CompanionVoice` / `UserCompanionConfig` 实体与 Mapper（对齐现表字段）
- [x] 1.2 新增 JWT `sub` → `user_id` 读取组件；缺失时拒绝
- [x] 1.3 `TencentTtsParamMapper`（或等价）量纲映射 + 单测

## 2. Catalog APIs

- [x] 2.1 `GET /api/companions` Controller/Service/DTO；仅 enabled；JWT
- [x] 2.2 `GET /api/companion-voices`（可选 `?businessCode=`）合并预设+可用定制；含 `used`；不返回 `requiresSubscription`
- [x] 2.3 Resource Server 将上述路径设为 `authenticated()`
- [x] 2.4 契约/切片测试：鉴权失败与成功列表

## 3. User config upsert

- [x] 3.1 `POST /api/user-companion-configs` 请求校验与音频缺省（1/50/100）；文案空白存 null；字数限制
- [x] 3.2 音色必选：`local` / preset `voiceBusinessCode` / custom `voiceBusinessCode`；无默认 `gentle_female`
- [x] 3.3 `(user_id, companion_id)` upsert 落库；成功仅信封（无摘要 `data`）；刷新 Redis
- [x] 3.4 `GET .../audio` 与 `GET .../interaction`（`configured`）
- [x] 3.5 测试：首次插入、再次更新、未选音色/非法音色 400、local 落库空串

## 4. Join linkage

- [x] 4.1 `JoinConvoaiRequest`/`Command` 增加可选 `businessCode`
- [x] 4.2 `ConvoaiService.join`：有 code 时合并用户配置注入 TTS/欢迎语/安抚；local 用 yml `voice_type`；idle 用全局配置（默认 180）
- [x] 4.3 测试：无 code 保持 yml；有配置覆盖；未知 code 400

## 5. Docs and verify

- [x] 5.1 更新 `README.md`、`docs/convoai-client-integration.md`、`docs/apifox-companion-config-test.md` 与本 change specs
- [ ] 5.2 `mvn spotless:apply` 与 `mvn verify`
