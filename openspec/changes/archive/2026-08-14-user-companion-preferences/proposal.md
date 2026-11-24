## Why

UE5 配置页需要按登录用户保存角色、音色、微调与欢迎语/安抚，并在进房时注入 ConvoAI。当前仅有 join/leave，TTS 与话术只读 yml，无法支撑 per-user 偏好。在已有 OpenID JWT 分支上补齐目录只读、启动 upsert 与 join 联动。

## What Changes

**角色与音色目录**
- From: 无陪伴目录 API
- To: `GET /api/companions`（含 `used`）、`GET /api/companion-voices`（可选 `?businessCode=`；启用预设 + 可用定制；含 `used`；JWT）
- Reason: 配置页选角色/音色
- Impact: non-breaking 新接口

**用户偏好保存与回显**
- From: 无用户配置写/读接口
- To: `POST /api/user-companion-configs` upsert（音色必选：`local` / `voiceBusinessCode`）；成功仅统一信封（无落库摘要 `data`）；`GET .../audio` 与 `GET .../interaction`（无角色 query；按 JWT `sub` 最近启动；`configured`）
- Reason: 启动一次落库，Tab 回显，成功后再进房
- Impact: non-breaking 新接口；相对早期草案为破坏性修正（无默认音色、无 `customVoiceId` 选型；audio/interaction 不再按角色 query）

**join 联动**
- From: join 仅 yml TTS/欢迎语
- To: 可选 `businessCode`；有则按用户配置覆盖 TTS/欢迎语/安抚；local 用 yml `voice_type`；idle 全局默认 180
- Reason: 进房使用已保存偏好
- Impact: 缺省 join 行为兼容；带 `businessCode` 时依赖已保存配置

## Capabilities

### New Capabilities

- `companion-catalog`: 启用角色与预设音色列表 API
- `user-companion-config`: 用户×角色偏好 upsert 与校验规则
- `convoai-companion-join`: join 可选 businessCode 时合并用户配置注入

### Modified Capabilities

- （无；join 扩展记入 `convoai-companion-join`，不修改已归档 TRTC 规格）

## Impact

- 代码：`api.companion`、`companion`、persistence entity/mapper、`ResourceServerSecurityConfig`（新路径 authenticated）、`ConvoaiService` / join DTO
- 文档：`README.md`、`docs/convoai-client-integration.md`
- DB：假定 `companion` / `companion_voice` / `user_companion_config` 已部署；本 change 不建表
- 鉴权：依赖现有 Jupiter Users JWT；`user_id` 取 `sub`
