## Design Summary

为陪伴/次元音色配置页提供**合成音色试听**：首次点击调用腾讯云 TextToVoice（`Codec=pcm`）并把 PCM 写入已建好的 `voice_preview_audio`；之后按「产品域 + 功能类型 + 音色雪花 id」命中缓存直接返回。前端只传音色 **`businessCode`**（与列表/upsert/定制一致）；后端查目录表解析出雪花 `id` 再读写试听表。**表结构已建好，本期不改 DDL。** 试听文案全局固定一句（yml）。不把 PCM 嵌进现有 voices 列表。

## Alternatives Considered

### Alternative A: 试听表直接以 `business_code` 为缓存键

- **Approach**: 改表列 `voice_ref_id` → `business_code`，API 与表同键。
- **Pros**: 少一次目录解析。
- **Cons**: 与已建表不一致；code 跨域碰撞需靠 scope；与配置表内部雪花 FK 风格分叉。
- **Why not chosen**: 表已按雪花 id 建好；产品要求表不改。

### Alternative B: API 传雪花 id，表直接查

- **Approach**: 前端从列表取 `id` 请求试听。
- **Pros**: 后端少一步。
- **Cons**: 次元列表无 id；陪伴 upsert 用 `voiceBusinessCode`；定制对外也是 code。
- **Why not chosen**: 与现行对外契约不一致。

### Alternative C: 缓存键雪花 id + API 入参 businessCode（应用层翻译）

- **Approach**: 表仍 `voice_ref_id`；API 收 `businessCode` + `productScope` + `voiceKind`；Service 先解析目录再查缓存；未命中 TextToVoice 后 INSERT。
- **Pros**: 对齐已建表与对外 code；唯一键不变；定制可顺带校验归属。
- **Cons**: 每次多一次目录查询（可接受）。
- **Why not chosen**: （选中）

## Agreed Approach

**Alternative C**：表不动；独立试听 API；`businessCode` → 雪花 id → `voice_preview_audio`；PCM 落库不进 COS；全局固定试听文案。

## Key Decisions

1. **表**：`voice_preview_audio` 已存在；唯一键 `(product_scope, voice_kind, voice_ref_id)`；无 `business_code` 列。
2. **API 入参**：`businessCode` + `productScope`（`companion`|`dimension`）+ `voiceKind`（`preset`|`custom`）；JWT Bearer。
3. **解析**：companion+preset → `companion_voice`；companion+custom → `companion_custom_voice`（须属 JWT `sub`）；dimension+preset → `dimension_voice`；dimension+custom → 拒绝（暂无表）。
4. **缓存未命中**：TextToVoice（`Codec=pcm`，`SampleRate=16000`，固定文案）→ INSERT（含 `sample_text`、可选 `synthesis_params`）→ 返回；并发冲突靠唯一索引再查一次。
5. **文案（已定）**：`树影婆娑，阳光正好。让声音为你打开一段温柔时光。`（yml 配置，全局共用）。
6. **响应**：统一 `ApiResponse`；`data` 含 base64 PCM + `sampleRate` + `codec`（不破坏信封约定）。
7. **TTS 客户端**：新增 infrastructure TTS Client，复用 `tencent.credentials` + 现有 TTS `app-id`；不把密钥写进业务包。
8. **Non-goals**：改 DDL、voices 列表嵌 PCM、PCM 存 COS、次元定制、文案变更自动清缓存、`synthesis_params` 参与唯一键、Flyway。

## Open Questions

- 无（路径形状与 HTTP method 在 design/proposal 定为 `GET /api/voice-previews` + query）。
