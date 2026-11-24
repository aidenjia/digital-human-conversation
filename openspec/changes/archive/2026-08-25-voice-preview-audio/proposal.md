## Why

配置页需要在选用音色前试听真实合成效果。当前仅有音色元数据列表与定制语料试音路径，没有「按音色合成固定文案 PCM」的缓存与 API。共享库已建好 `voice_preview_audio`，本期补齐后端试听链路，避免每次点击都打腾讯并与列表契约解耦。

## What Changes

**音色试听 API**
- From: 无合成试听接口；voices 列表仅元数据
- To: `GET /api/voice-previews`（JWT）；Query：`businessCode` + `productScope` + `voiceKind`；`data` 含 base64 PCM、`sampleRate`、`codec`
- Reason: 前端用对外 `businessCode` 试听；列表保持轻量
- Impact: 非破坏性新增；前端可选接入

**缓存与合成**
- From: 无试听缓存读写
- To: 目录表按 `business_code` 解析雪花 id → 查/写 `voice_preview_audio`；未命中调 TextToVoice（pcm/16000）后落库
- Reason: 首次生成、之后读库；表结构已存在不改 DDL
- Impact: 依赖腾讯 TTS；并发靠唯一索引再查

**配置**
- From: 无试听文案配置
- To: yml 固定文案「树影婆娑，阳光正好。让声音为你打开一段温柔时光。」+ 采样率等
- Impact: 非密钥配置；CD 可不改即可用默认

**鉴权范围**
- From: `/api/voice-previews` 未纳入受保护路径
- To: 与其他 `/api/**` 一样要求 Bearer JWT
- Impact: 修改 `openid-jwt-validation` 受保护路径说明（若规格枚举路径）

## Capabilities

### New Capabilities
- `voice-preview-audio`: 音色试听（businessCode 解析、PCM 缓存、TextToVoice 首点生成）API 与编排

### Modified Capabilities
- `openid-jwt-validation`: 受保护路径增加 `/api/voice-previews`（若现行规格以路径列表描述）

## Impact

- 代码：`api.voicepreview`、`voicepreview` 编排、TTS Client（infrastructure）、Entity/Mapper、`ResourceServerSecurityConfig`
- API：新增 `GET /api/voice-previews`
- DB：仅用已有 `voice_preview_audio`（无迁移）
- 依赖：腾讯云 TTS SDK 或 REST（与 VRS/COS 共用 `tencent.credentials`）
- 文档：README；可选 `docs/sql/voice-preview-audio.sql` 备份 DDL
- 测试：缺 token / 无效 code / 定制非本人 / 缓存命中 / 未命中合成（mock TTS）
- 部署：replicas=2 无粘滞；无新中间件
