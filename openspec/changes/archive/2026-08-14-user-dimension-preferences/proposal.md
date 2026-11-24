## Why

Jupiter SR 次元数字人需要按登录用户保存预设音色、微调、欢迎语/安抚和人设，并在 MMD 进房时注入 ConvoAI。当前仅有陪伴（用户×角色）配置，人设来自角色目录且 Redis 只有 `companion:{sub}`。次元没有服务端角色、不要定制音色、不要改陪伴表，必须用隔离的目录与按 userId 一行的配置。

## What Changes

**次元预设音色目录**
- From: 无次元音色 API（陪伴 `GET /api/companion-voices` 含定制、可带角色 query）
- To: `GET /api/dimension-voices`：仅启用的 `dimension_voice`；含 `used`；JWT；无定制、无角色 query
- Reason: 两产品音色上下架独立
- Impact: non-breaking 新接口

**按用户配置与回显**
- From: 无人设/次元偏好 API
- To: `POST /api/user-dimension-configs` 按 `sub` upsert 一行（音色必选：local 或预设 `voiceBusinessCode`）；`GET .../audio|interaction|persona`（`configured`）；成功仅信封
- Reason: 人设绑用户；启动一次落库
- Impact: non-breaking 新接口

**MMD join 联动**
- From: 无 `businessCode` 时 join 只用 yml；有 code 走陪伴
- To: `digitalHumanType=MMD` 且无 `businessCode` 时合并 `user_dimension_config`（TTS/欢迎/安抚）；人设写入 Redis `dimension:{sub}`；idle 仍全局 180
- Reason: 进房使用次元偏好且不冲陪伴缓存
- Impact: 非 MMD、带 `businessCode` 的 join 保持现状

## Capabilities

### New Capabilities

- `dimension-voice-catalog`: 次元启用预设音色列表
- `user-dimension-config`: 按 userId 的偏好 upsert、校验与回显
- `convoai-dimension-join`: MMD join 合并次元配置并写独立 Redis

### Modified Capabilities

- （无；不修改已归档 `convoai-join` 正文，扩展记入 `convoai-dimension-join`）

## Impact

- 代码：`api.dimension`、`dimension`、persistence、`ResourceServerSecurityConfig`、`ConvoaiService` 分流
- 文档：`README.md`、`docs/convoai-client-integration.md`
- DB：共享库新增 `dimension_voice`、`user_dimension_config`（本仓不跑 Flyway）
- Redis：新 key `dimension:{sub}`；agent 读取为跨仓依赖
- 鉴权：现有 Users JWT；`user_id`=`sub`
