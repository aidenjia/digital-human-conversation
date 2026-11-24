# Text Input Voice Reply Implementation Plan

> **For agentic workers:** Use executing-plans inline, preserving the current feature branch and its existing uncommitted avatar work. No delegation or unrelated commits.

**Goal:** 默认文字提问，PCM / WebRTC 返回语音及字幕，不申请麦克风权限。

**Architecture:** 页面选择输入模式，hooks 编排字幕及播放。后端统一验证 text.submit 并串行取消旧响应、提交 user input_text、触发 audio/text response；动作模型直接接收文字。

**Tech Stack:** React / TypeScript / browser WebRTC / Web Audio, Java / Spring / Jackson / JUnit / Mockito.

## Global Constraints

- 保留语音模式、欢迎语及现有动作冷却规则；会话开始前选择模式。
- 不复制或重置当前未提交修改，不变更模型与 API 密钥配置。
- 中文输入法 Enter 不发送；发送失败恢复输入；过期回复不得污染下一轮。
- 单元测试不能代替真实浏览器播放验收。

### Task 1: 受控文字提交与轮次控制

Files: `src/main/java/aidenjia/digitalhuman/conversation/bailian/support/RealtimeTextTurns.java`, 两个 `BailianRealtime*Session.java`, `infrastructure/bailian/BailianRealtimeUpstreamClient.java`, `avatar/AvatarDecisionSession.java` 及对应 tests。

Interface: 客户端 `{type:'text.submit',id:string,text:string}`；服务端 `{type:'text.accepted'|'text.rejected',id:string,message?:string}`。输入限制 4000 字符，id 最长 80 字符；单连接去重。提交期间旧响应取消后再创建新响应，避免 response.create 与旧生成并行。

- [x] RED: 测试未就绪/空白拒绝、正常提交、重复 id、旧轮取消及迟到事件过滤、动作上下文。初始 4 项行为失败；补充去重和旧完成重放测试也分别观察到失败。
- [x] Run: focused Maven suite，初始格式门禁跳过后确认真实行为失败（`target/text-input-red.log`）。
- [x] GREEN: 编码 input_text 用户消息与 audio/text response.create；控制层只接收受控提交，保留服务端密钥。协议编码封装在 RealtimeTextTurns 中，未修改既有上游客户端。
- [x] 重跑相关测试及完整 verify，确认通过。

### Task 2: 文字模式媒体与发送客户端

Files: `conversation-ui/src/services/realtimeText.ts`, `bailianRealtimeWs.ts`, `bailianRealtimeEventsWs.ts`, `bailianWebRtcSession.ts`, 两个 conversation hooks，及 `conversation-ui/tests/realtime-text.test.ts`。

Interface: `InputMode = 'text' | 'voice'`，`startConversation(model,persona,voice,inputMode)`，`sendText(text):Promise<void>`。WebRTC text offer 不使用 getUserMedia；PCM text 不启动麦克风或采集。

- [x] RED: 输入规范化、IME / Enter、提交回执 / 拒绝 / 关闭、无麦克风 offer 与打断测试。
- [x] Run: `node --test tests/realtime-text.test.ts`，首次因模块尚未实现而失败；WebRTC 媒体测试分别观察到 microphone denied 和未启动 RTP 播放的真实行为失败。
- [x] GREEN: 请求回执管理；提交后暂停旧播放并在新响应开始后恢复；以 responseId 过滤过期事件；每问独立字幕。另补 PcmPlayback 与 captionLines 及对应测试。
- [x] Run: `node --test tests/*.test.ts`，40 项通过。

### Task 3: 页面与整体验收

Files: `conversation-ui/src/ConversationRealtime.tsx`, `conversation-ui/src/Conversation.css`, `docs/avatar-decision.md`。

- [x] 添加默认文字模式选择和连接后输入框，禁止空白和未连接发送；Enter / Shift+Enter / IME 由已测 helper 判断。
- [x] 两种传输中验证文字问题、长回复、连续提问及中途打断；WebRTC 另测“你好”。既有语音相关自动回归通过。
- [ ] 人工补充真人语音输入回归与实际声学听感，不以电平/协议证明听感或逐字同步。
- [x] Run frontend: `node --test tests/*.test.ts`, `npm run lint`, `npm run build`。
- [x] Run backend: Spotless / focused suite / `mvn -o verify`（JVM 使用本机 ByteBuddy startup agent）。
- [x] 浏览器使用 Chrome；证据、后端重启需求及未完成边界见 `docs/text-input-voice-reply.md`。功能文件未自动提交或推送。
