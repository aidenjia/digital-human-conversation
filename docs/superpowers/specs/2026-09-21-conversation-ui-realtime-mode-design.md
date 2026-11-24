# conversation-ui 双模式（Realtime / 旧版 ConvoAI）设计

**日期：** 2026-09-21  
**状态：** 已确认  
**范围：** 仅 `conversation-ui` 测试页；后端 RTC / Bailian 接口已存在，本期不改后端。

## 背景

后端已提供：

- `POST /api/rtc/credentials` → `{ appId, channel, token, userId }`（无 agent）
- `POST /api/bailian/realtime-tokens` → `{ token, expiresAt, wsUrl, model, instructions, voice }`

契约见仓库 `README.md`「Bailian Realtime（Qwen-Audio）+ 声网 3A」：客户端直连百炼 WS；声网仅 3A / 播放参考；本服务不中继 WS。人设/音色本期为 yml 固定值。

现有 UI 仅支持 ConvoAI：`login → config → conversation`（`POST|DELETE /api/convoai/agents` + Agora 订阅远端 Agent）。

## 目标

1. 登录保持现状（短信 + Users JWT）。
2. 登录成功后选择：**Realtime（Qwen-Audio）** 或 **旧版语音通话（ConvoAI）**。
3. Realtime **跳过** DigitalHumanConfig，直接进入通话页。
4. 验证「声网 3A + 百炼 WS」端到端可用性。
5. 旧版路径行为不变。

## Non-goals

- 服务端 WebSocket 中继
- 陪伴/次元人设、欢迎语注入 Realtime
- 临时 Key 无感续签（到期提示重开即可）
- 替换生产客户端 / UE / Unity
- 修改后端 API 或 yml 契约

## 决策

| 项 | 选择 | 理由 |
|----|------|------|
| 架构 | 方案 A：模式选择 + 双 Conversation | 旧路径隔离，联调风险低 |
| Realtime 配置页 | **跳过** | 后端固定 instructions/voice，配置页无效果 |
| 持久化 | `sessionStorage.test_voice_mode = 'realtime' \| 'legacy'` | 与现有 test_* 并列 |
| 刷新恢复 | realtime → 直进 Realtime 页；legacy + configSaved → 旧 conversation；否则 mode | 避免 Realtime 误进配置 |

## 阶段机

```
AppStage =
  | 'login'
  | 'mode'                 // 新增
  | 'config'               // 仅 legacy
  | 'conversation'         // 仅 legacy ConvoAI
  | 'conversation-realtime'// 新增
```

流转：

```
Login --success--> ModeSelect
ModeSelect --legacy--> DigitalHumanConfig --> Conversation
ModeSelect --realtime--> ConversationRealtime
Conversation --back--> ModeSelect（或保留「回配置」仅 legacy）
ConversationRealtime --back--> ModeSelect
401 / logout --> login（clearTestSession 含 voice_mode）
```

## Realtime 通话序列

1. `POST /api/rtc/credentials`（Bearer JWT，空 body）
2. Agora `join` → 创建麦克风轨（保持默认 3A）→ `muteLocalAudioStream(true)`（不推麦给频道他人）
3. `POST /api/bailian/realtime-tokens`
4. `WebSocket`：浏览器无法设置 `Authorization` Header，**开发/预览**经 Vite 本机路径 `/__bailian_ws` 握手代理注入 Bearer 后再连百炼（见 `vite.bailian-ws-proxy.ts`；**不是**业务服务 WS 中继）
5. 发送 `session.update`：`instructions` / `voice`（用响应字段）、`turn_detection.type=server_vad`、`input_audio_format=pcm`、`output_audio_format=pcm`（百炼约定：上行 16 kHz / 下行 24 kHz，16-bit mono）
6. 上行：从本地麦轨取 PCM → 重采样 **16 kHz** → `input_audio_buffer.append`（base64；约每 20–40 ms 一块）
7. 下行：`response.audio.delta` → PCM **24 kHz** → AudioContext + Agora `createCustomAudioTrack().play()`（AEC 参考）+ 本机扬声器
8. 结束：关闭 WS → 关麦 → Agora `leave`；**禁止**调用 `/api/convoai/agents`

推流静音：Agora NG SDK 使用 `localAudioTrack.setMuted(true)`（等价 README 的 `muteLocalAudioStream(true)`）。

## 模块边界

| 模块 | 职责 |
|------|------|
| `pages/ModeSelect.tsx` | 两入口：Realtime / 旧版 |
| `apis/rtc.ts` / `apis/bailian.ts` | HTTP 客户端 + 类型 |
| `services/bailianRealtimeWs.ts` | WS 生命周期、session.update、append、事件、心跳 |
| `hooks/useRealtimeConversation.ts` | 编排 RTC + WS；connecting / live / error |
| `ConversationRealtime.tsx`（或 `pages/`） | UI：开始 / 静音 / 结束 / 回模式选择 |
| `agoraRTC.ts` 扩展 | muteLocal、采 PCM、下行自定义播放（Realtime 专用方法，不破坏旧路径） |
| `session.ts` | `get/set/clearVoiceMode`；`clearTestSession` 一并清理 |
| `App.tsx` | 新阶段 + 登录后进 `mode` |

**禁止：** 在 `useConversation` 内分支 Realtime；旧 ConvoAI 主路径保持外科手术式不动。

## 安全与日志

- 禁止 `console` / UI 打印临时 Key、RTC token 全文、JWT。
- Realtime 两接口须真 Users JWT（demo Bearer 仅 agents，不可用）。

## 验收

1. 登录 → 选 Realtime → **不经**配置页 → 可开始通话。
2. Network 无 `/api/convoai/agents`；有 credentials + realtime-tokens；WS 至 `*.maas.aliyuncs.com`。
3. 开麦说话有轮次；可听到模型回复。
4. 选旧版 → 配置 → ConvoAI 进房仍可用。
5. 401 / 退出清 session 回登录。

## 风险

| 风险 | 缓解 |
|------|------|
| Agora PCM 采集 / 自定义播放 API 差异 | 钉现有 `agora-rtc-sdk-ng`；卡点记录并最小可用降级 |
| 16 kHz 重采样 | 固定单一 resampler |
| 旧路径回归 | 模式严格分流，不改 `useConversation` 主逻辑 |
