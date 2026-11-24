## Design Summary

在 JWT 鉴权分支上为 UE5 配置页提供用户偏好能力：角色/音色列表、启动一次 upsert 落库，进房按 `businessCode` 合并用户配置注入 TTS/欢迎语/安抚。表已在共享 Postgres 存在，本 change 不建表。

> **Superseded（相对初版 Key Decisions，以 `design.md` / delta specs / 现码为准）**  
> 省略音色默认链 → 未选音色 400；官方话术落库 → 空白存 null；idle 300 → 180；不做定制/回显 → 已做合并定制列表 + audio/interaction GET；`customVoiceId` 选型 → `voiceBusinessCode`；支持 `voiceSource=local`。

## Alternatives Considered

### Alternative A: 角色/音色行上挂欢迎语与微调 + 多 PATCH
- **Approach**: 欢迎语挂 `companion`，微调挂 `companion_voice`，多个写接口。
- **Pros**: 实现快，少一张用户表。
- **Cons**: 改一次全局影响所有用户；与 per-user 产品冲突。
- **Why not chosen**: 产品要求按登录用户隔离偏好。

### Alternative B: 启动与 join 合成一个接口
- **Approach**: 一次请求既落库又创建 ConvoAI agent。
- **Pros**: 调用方更少一步。
- **Cons**: 失败语义难拆；与现有 join/leave 契约冲突。
- **Why not chosen**: 保留现有进房/离房，启动只负责 upsert。

### Alternative C: 用户偏好 + 试听/定制一并交付
- **Approach**: 同期做 TTS 试听与定制音色工单。
- **Pros**: UI 全亮。
- **Cons**: 依赖 TTS/支付/素材，范围膨胀。
- **Why not chosen**: 本期不做试听与定制**创建**工单；选用已完成定制音色已纳入列表/upsert。

## Agreed Approach

采用 **用户配置表 upsert + 只读目录列表 + 改进房联动**：  
`GET /api/companions`、`GET /api/companion-voices`、`GET .../audio|interaction`、`POST /api/user-companion-configs`；`POST /api/convoai/agents` 增加可选 `businessCode`，用 JWT `sub` 作为 `user_id`。

## Key Decisions（现行）

1. `user_id` = JWT claim `sub`；请求体不传 userId；不做登录发 token。
2. 角色对外键 = `businessCode`；云端音色 = `voiceBusinessCode`；本地 = `voiceSource=local`（落库 `""`）。
3. 未选音色 → 400；无默认 `gentle_female`。
4. TTS 音频缺省：语速 1 / 音量 50 / 音高 100；欢迎语/安抚开关默认 true，文案空白存 null；字数 ≤100 / ≤50。
5. 音色只校验上架/可用；无订阅校验；响应不暴露 `requiresSubscription`。
6. idle_timeout 全局配置默认 180，不入库。
7. 包归属：`api.companion` / `companion` / persistence；join 联动改 `ConvoaiService`。
8. 新接口须 `authenticated()`（与 ConvoAI 同 JWT）。

## Open Questions

- 无。
