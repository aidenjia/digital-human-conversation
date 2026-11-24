## Why

先前把次元人设和音色只挂在 `userId` 上，与 Jupiter SR 原型不符：人设、音色必须跟当前次元角色走。微调与互动仍是用户级。现在纠正模型，让配置页能按角色选人设/音色，同时保持每用户一行、不引入外键。

## What Changes

**次元角色目录**
- From: 无角色表；人设仅 `GET /api/user-dimension-configs/persona`
- To: `dimension_character` + `GET /api/dimension-characters`（`businessCode`、`name`、`used`、每项 `personaText` 来自目录）
- Reason: 人设跟角色列表走，对标陪伴角色列表
- Impact: 新接口；删除 GET persona（破坏性）

**配置行绑定当前角色**
- From: `user_dimension_config` 仅按 `user_id`；POST 不传角色
- To: 逻辑列 `dimension_character_id`（无 FK）；POST 必带次元角色 `businessCode`；仍 upsert 同一行
- Reason: 同时只有一个角色/音色/一套参数
- Impact: POST 破坏性（缺角色 code → 400）

**音色 used 按角色**
- From: `GET /api/dimension-voices` 无 Query，`used` 只看用户行音色
- To: Query `businessCode` 必填；`used` 仅当该角色即当前配置角色且选中该预设
- Reason: 换角色换音色
- Impact: 破坏性（缺/非法 code → 400）

**不变**
- GET audio / interaction 无 Query；MMD join 仍按 `sub`；无定制声、无 idle、不改陪伴

## Capabilities

### New Capabilities

- `dimension-character-catalog`: 启用次元角色列表（含 `used` 与当前人设回显）

### Modified Capabilities

- `user-dimension-config`: POST 必带角色 `businessCode`；删除 GET persona；audio/interaction 不变
- `dimension-voice-catalog`: voices 必带角色 `businessCode` 才能标 `used`
- `openid-jwt-validation`: 受保护路径增加 `/api/dimension-characters`

## Impact

- 代码：`api.dimension`、`dimension`、persistence Entity/Mapper、`ResourceServerSecurityConfig`、鉴权测试
- 文档：`README.md`、`.cursor/rules/java-api-convoai.mdc`、DDL 说明
- DB：共享库新建 `dimension_character`；`user_dimension_config` 加可空 `dimension_character_id`（无 FK）
- Redis / join：行为不变（upsert 仍写 `dimension:{sub}`；join 仍读用户行）
- 客户端：改角色列表取人设（每项目录人设）；voices 与 POST 带角色 code；POST 不传 `personaText`
