# conversation-ui Realtime 双模式 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `conversation-ui` 登录后增加模式选择；Realtime 跳过配置页，用 RTC credentials + 百炼 WS 验证 Qwen-Audio 实时通话；旧版 ConvoAI 路径保持不变。

**Architecture:** `App` 阶段机增加 `mode` / `conversation-realtime`。Realtime 独立 hook + WS client + Agora 扩展；旧 `useConversation` 不分支。协议对齐百炼：上行 PCM 16 kHz、下行 PCM 24 kHz、`session.update` + `input_audio_buffer.append` + `response.audio.delta`。

**Tech Stack:** React + Vite、agora-rtc-sdk-ng、原生 WebSocket、现有 `request.ts` Bearer 信封。

**Spec:** `docs/superpowers/specs/2026-09-21-conversation-ui-realtime-mode-design.md`

## Global Constraints

- Realtime **跳过** DigitalHumanConfig；登录成功 → ModeSelect。
- Realtime **禁止** 调用 `/api/convoai/agents`。
- 禁止日志/UI 打印临时 Key、RTC token、JWT 全文。
- 不改后端 Java；不引入服务端 WS。
- 旧版 `useConversation` / `Conversation.tsx` 尽量零改动（仅 App 接线与「回模式选择」导航若需要）。
- 百炼音频：`input_audio_format=pcm`（16 kHz）、`output_audio_format=pcm`（24 kHz）；事件名用文档字面量。

---

## File map

| 文件 | 职责 |
|------|------|
| Create `conversation-ui/src/pages/ModeSelect.tsx` | 双入口 UI |
| Create `conversation-ui/src/apis/rtc.ts` | `POST /api/rtc/credentials` |
| Create `conversation-ui/src/apis/bailian.ts` | `POST /api/bailian/realtime-tokens` |
| Modify `conversation-ui/types/api.d.ts` | `RtcCredentialsResponse` / `BailianRealtimeTokenResponse` |
| Create `conversation-ui/src/services/bailianRealtimeWs.ts` | WS 客户端 |
| Create `conversation-ui/src/utils/pcmBridge.ts` | base64 / 重采样辅助（若需要） |
| Modify `conversation-ui/src/services/agoraRTC.ts` | Realtime：muteLocal、采 PCM、下行播放 |
| Create `conversation-ui/src/hooks/useRealtimeConversation.ts` | 编排 |
| Create `conversation-ui/src/ConversationRealtime.tsx` (+ css 可复用 Conversation.css) | Realtime UI |
| Modify `conversation-ui/src/utils/session.ts` | `voiceMode` |
| Modify `conversation-ui/src/App.tsx` | 阶段机 |
| Modify `conversation-ui/README.md`（若有） | 双模式说明一行 |

---

### Task 1: session + ModeSelect + App 阶段机（无通话）

**Files:**
- Modify: `conversation-ui/src/utils/session.ts`
- Create: `conversation-ui/src/pages/ModeSelect.tsx`
- Modify: `conversation-ui/src/App.tsx`
- Modify: `conversation-ui/src/pages/Login` 成功回调目标（经 App）

**Interfaces:**
- Produces: `VoiceMode = 'realtime' | 'legacy'`；`getVoiceMode` / `setVoiceMode` / `clearTestSession` 清 mode
- Produces: `AppStage` 含 `mode` | `conversation-realtime`
- Produces: `resolveInitialStage()`：未登录→login；`voiceMode=realtime`→conversation-realtime；`voiceMode=legacy && isConfigSaved`→conversation；有登录无 mode→mode；legacy 无配置→mode（不要默认进 config，避免跳过选择）

- [ ] **Step 1:** 扩展 `session.ts`：`VOICE_MODE_KEY = 'test_voice_mode'`；读写与 clear。
- [ ] **Step 2:** 实现 `ModeSelect`：两按钮「实时语音（Qwen-Audio）」「旧版语音（ConvoAI）」；回调 `onSelect(mode)`。
- [ ] **Step 3:** 改 `App.tsx`：`handleLoginSuccess → setStage('mode')`；选 realtime → `setVoiceMode` + `conversation-realtime` 占位页（可先显示「Realtime 占位」+ 返回）；选 legacy → `setVoiceMode` + `config`。
- [ ] **Step 4:** 手动验证：登录 → 模式页；选 Realtime 不进配置；选旧版进配置；退出清 mode。
- [ ] **Step 5:** Commit（仅当用户要求时）：`feat(conversation-ui): add voice mode select after login`

---

### Task 2: RTC / Bailian HTTP API + 类型

**Files:**
- Modify: `conversation-ui/types/api.d.ts`
- Create: `conversation-ui/src/apis/rtc.ts`
- Create: `conversation-ui/src/apis/bailian.ts`

**Interfaces:**
- Produces:
  ```ts
  // api.d.ts
  interface RtcCredentialsResponse {
    appId: string; channel: string; token: string; userId: string
  }
  interface BailianRealtimeTokenResponse {
    token: string; expiresAt: number; wsUrl: string
    model: string; instructions: string; voice: string
  }
  ```
- Produces: `fetchRtcCredentials(): Promise<BaseRes<RtcCredentialsResponse>>`
- Produces: `fetchBailianRealtimeToken(): Promise<BaseRes<BailianRealtimeTokenResponse>>`
- 使用现有 `request`（自动 Bearer）；POST 空 body 或 `{}`

- [ ] **Step 1:** 加类型与两个 API 函数（对齐 `apis/index.ts` 风格）。
- [ ] **Step 2:** 浏览器 Network（已登录）各调一次，确认 200 与字段；失败时确认非 demo token 问题。
- [ ] **Step 3:** Commit（用户要求时）：`feat(conversation-ui): add rtc and bailian token API clients`

---

### Task 3: Bailian WebSocket 客户端

**Files:**
- Create: `conversation-ui/src/services/bailianRealtimeWs.ts`

**Interfaces:**
- Produces:
  ```ts
  type BailianWsHandlers = {
    onOpen?: () => void
    onSessionUpdated?: () => void
    onAudioDelta?: (pcm24k: ArrayBuffer) => void
    onSpeechStarted?: () => void
    onSpeechStopped?: () => void
    onError?: (message: string) => void
    onClose?: () => void
  }
  class BailianRealtimeWs {
    connect(wsUrl: string, tempToken: string, handlers: BailianWsHandlers): Promise<void>
    updateSession(opts: { instructions: string; voice: string }): void
    appendPcm16kBase64(audioBase64: string): void
    close(): void
  }
  ```
- `session.update` 建议 payload：
  ```json
  {
    "type": "session.update",
    "session": {
      "modalities": ["text", "audio"],
      "instructions": "<from API>",
      "voice": "<from API>",
      "input_audio_format": "pcm",
      "output_audio_format": "pcm",
      "turn_detection": { "type": "server_vad", "threshold": 0.5, "silence_duration_ms": 800 }
    }
  }
  ```
- 解析 `response.audio.delta`（字段名以官方文档为准，实现时对照 [客户端事件](https://www.alibabacloud.com/help/zh/model-studio/fun-audiochat-client-events)）；audio 为 base64 → `ArrayBuffer`。
- 浏览器 `WebSocket`：**无法设自定义 Header**。若浏览器环境无法传 `Authorization`，改用文档支持的 query/`protocols` 方案，或在注释中记录并改用临时方案（实现前用 Network/文档确认一种可行鉴权）。**实现时必须先验证浏览器鉴权方式**，卡点写进 README。
- 禁止 log `tempToken`。

- [ ] **Step 1:** 实现 connect / updateSession / append / close / 事件分发。
- [ ] **Step 2:** 用临时脚本或 Realtime 页「仅连 WS」按钮验证：`session.created` → `session.updated`（可先不传麦）。
- [ ] **Step 3:** Commit（用户要求时）：`feat(conversation-ui): add Bailian realtime WebSocket client`

**鉴权注意（浏览器）：** 原生 WebSocket 不能设 Header。实现前查百炼文档是否支持 URL query `api_key` / subprotocol。若仅支持 Header，可选：`npm` 包或通过短暂验证——以文档为准，并在本 Task 结束时写明采用的方式。

---

### Task 4: Agora Realtime 扩展（mute + PCM 桥）

**Files:**
- Modify: `conversation-ui/src/services/agoraRTC.ts`
- Create（可选）: `conversation-ui/src/utils/pcmBridge.ts`

**Interfaces:**
- Produces（Realtime 专用，不破坏旧 join/subscribe）：
  - `muteLocalAudioStream(muted: boolean): Promise<void>`
  - `startPcmCapture(onChunk: (pcm16k: Int16Array) => void): Promise<void>` — 从本地麦轨经 AudioContext 抽 PCM，重采样到 16 kHz
  - `stopPcmCapture(): void`
  - `playPcm24k(chunk: ArrayBuffer): void` — 优先自定义音频轨进 Agora 播放通路；若 SDK 限制，先本地 AudioContext 播放并 TODO 标注与 README 差距
- 旧路径方法签名不变。

- [ ] **Step 1:** 实现 muteLocal（join + enableMic 之后调用）。
- [ ] **Step 2:** 实现 16 kHz 采集循环（约 20–40 ms / ~640–1280 samples @16k）。
- [ ] **Step 3:** 实现 24 kHz 下行播放。
- [ ] **Step 4:** 单元级/手测：进房 mute 后频道无上行推流（或 sendBytes 不增长）；采集有 PCM 能量。
- [ ] **Step 5:** Commit（用户要求时）：`feat(conversation-ui): agora pcm bridge for realtime mode`

---

### Task 5: useRealtimeConversation + ConversationRealtime UI

**Files:**
- Create: `conversation-ui/src/hooks/useRealtimeConversation.ts`
- Create: `conversation-ui/src/ConversationRealtime.tsx`
- Create/Reuse: CSS（可 import `Conversation.css`）
- Modify: `conversation-ui/src/App.tsx` — 挂载真页面
- Modify: `conversation-ui/src/hooks/index.ts`（若有导出）

**Interfaces:**
- Produces hook API 对齐旧页体验：
  ```ts
  {
    isConnected: boolean
    isMicEnabled: boolean
    isStarting: boolean
    error: string | null
    sessionMeta: { model?: string; voice?: string } | null
    startConversation: () => Promise<void>
    endConversation: () => Promise<void>
    toggleMic: () => Promise<void>
  }
  ```
- `startConversation` 顺序：credentials → Agora join → enableMic → muteLocal(true) → bailian token → WS connect → session.update → startPcmCapture → append
- `endConversation`：stop capture → ws.close → disableMic → leave；无 agents DELETE
- `toggleMic`：停/启 append（及轨 enabled），不 unpublish 破坏 3A 亦可先简单 setEnabled

- [ ] **Step 1:** 实现 hook。
- [ ] **Step 2:** 实现 UI：开始 / 静音 / 结束 / 返回模式选择；展示 model/voice/状态（勿展示 token）。
- [ ] **Step 3:** App 接线 `conversation-realtime`。
- [ ] **Step 4:** 端到端手测验收清单（见 Spec）。
- [ ] **Step 5:** Commit（用户要求时）：`feat(conversation-ui): realtime qwen-audio conversation page`

---

### Task 6: 文档与联调清单

**Files:**
- Modify: `conversation-ui/README.md`（若存在）或根 README 测试页小节一行
- Optional: Spec 验收勾选

- [ ] **Step 1:** 写清：双模式、Realtime 跳过配置、须真 JWT、浏览器 WS 鉴权方式、已知限制（下行是否完全走声网）。
- [ ] **Step 2:** 自测勾选 Spec 验收 1–5。
- [ ] **Step 3:** Commit（用户要求时）：`docs: conversation-ui realtime mode usage`

---

## 手工验收清单

1. [ ] 登录 → ModeSelect
2. [ ] Realtime：无配置页；无 `/api/convoai/agents`
3. [ ] Network：`/api/rtc/credentials`、`/api/bailian/realtime-tokens`、WS 到 maas
4. [ ] 说话有 VAD / 模型回复可听
5. [ ] 旧版：配置 → ConvoAI 进房正常
6. [ ] 退出 / 401 清 session 回登录

## 风险卡点（实现时优先处理）

1. **浏览器 WebSocket 无法设置 Authorization Header** — Task 3 第一步必须定鉴权方案。
2. **下行 24 kHz → 声网自定义轨** — 若受阻，先本地播放保证可测，并在 README 标明与 Spec 差距。
