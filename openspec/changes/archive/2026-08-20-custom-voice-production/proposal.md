## Why

配置页已能选用「已完成」的定制音色并进房，但缺少从录音到腾讯训练落库的生产链路。桌面《声音复刻》方案与现库「用户资产」模型已对齐需求：本期不做支付，走腾讯**一句话复刻（TaskType=5）**并落库数字 VoiceType 供 join，补齐生产 API 才能端到端定制。

## What Changes

**定制音色生产 API**
- From: 仅能列表/upsert 已完成的 `companion_custom_voice`；无建单与腾讯 VRS 编排
- To: 对齐产品一页定制：`training-text`（不建单）→ 用户录音上传 → `POST` 提交（建单 `custom_00N`+检测+训练）→ `GET` 轮询；写回训练相关字段
- Reason: 必须用腾讯文案再录，且符合产品 UI
- Impact: non-breaking 新接口；依赖已部署列与腾讯凭证

**支付门禁（本期策略）**
- From: 产品期望先支付再制作
- To: 建单直接 `pay_status=2`；不接星核支付
- Reason: 需求明确本期不做支付，仍满足库 CHECK
- Impact: 后续接支付时改为真实置 2；客户端勿假设已扣费

**朗读文案**
- From: 页面写死「文案样例」
- To: 正式检测用文案来自腾讯 `GetTrainingText`（经本服务 API）；静态样例仅说明用
- Reason: 检测必须对齐 TextId
- Impact: 前端制作流程需调拉文案接口

## Capabilities

### New Capabilities

- `custom-voice-production`: 定制音色生产单生命周期（建单、文案、检测、训练、状态查询）及与腾讯 VRS 的编排约定

### Modified Capabilities

- （无；选用/join 行为不改需求，仍用既有 companion 配置能力）

## Impact

- 代码：新建 `api` + 业务编排包 + 腾讯 VRS infrastructure client；扩展 `CompanionCustomVoice` 实体映射；Security 新路径 authenticated；可选 Redis 存 AudioId
- 配置：腾讯 VRS 密钥 Properties + 环境变量 / CD（可与现 TTS 密钥同源或分项，实现时钉死）
- 文档：README、客户端对接文档、必要时 Apifox
- DB：假定 `companion_custom_voice` 已含 `training_text_id`、`provider_task_id`；本仓库不建表
- 鉴权：JWT `sub` = `user_id`
