# 文字提问、语音与字幕回复

## 使用

进入 WebRTC 或 PCM 中继页面，默认“文字输入（无需麦克风）”。点击“开始对话”后输入问题；Enter 发送，Shift+Enter 换行。中文输入法确认候选词不会发送。每条最多 4000 字，空白不发送。

数字人沿用当前人设和音色，返回语音及流式字幕；字幕按提问分行，发送新问题会打断旧回复。原语音输入可在开始会话前选择，会话中不可切换模式。文字模式不申请麦克风权限。

固定欢迎语只播放语音和字幕，不自动挥手，也不消耗动作冷却；用户问候才进入动作判断。用户回复中的实际自动挥手仍保留同类动作 30 秒冷却。

## 实现边界

- PCM 文字模式只连接中继 WebSocket 和初始化 Web Audio 播放，不获取声网入房凭证、不进房、不初始化采集。语音模式继续采用现有声网及 PCM 采集路径。
- WebRTC 文字模式使用 recvonly 音频 transceiver，不调用 getUserMedia。回复仍走原生 RTP；实际会话未观察到可用的 DataChannel PCM，不能直接照搬 WebSocket 播放方式。
- 打断时 PCM 停止全部已调度音源；WebRTC 暂停并解除旧 audio 元素的流绑定，取消旧生成后在新响应开始时重建播放元素，保留 PeerConnection 和远端媒体流。未做扬声器录音或逐样本残留音频测量，不承诺零毫秒声学残留。
- 后端验证 text.submit 的就绪状态、id、空白和长度，返回 text.accepted / text.rejected；text.started 表示开始提交新轮。回执不是模型完成或上游最终接受的证明。
- 同连接保留最近 128 个提交 id 去重，不自动重发。回执超时明确提示结果未知。
- 取消期间不并发创建新生成；过期 responseId 的音频、字幕和工具调用不会转入新轮。重复迟到的旧 response.done 也不能释放新轮等待。
- 用户文字直接进入 AvatarDecisionSession，不依赖 ASR，不修改动作冷却及提示词。
- 字幕更新采用纯函数，避免 React StrictMode 重放更新器时修改外部 ref 而丢行。

协议参考：[百炼 Qwen-Audio 客户端事件](https://help.aliyun.com/zh/model-studio/fun-audiochat-client-events)。本地参考原文：`bailian-docs-llm-wiki/raw/model-api-reference/audio-api-references/voice-conversation-api-references/real-time-voice-conversation-api-references/fun-audiochat-client-events.md`。

## 验收记录（2026-10-21）

- 后端完整离线 Maven verify 成功，含 Spotless、单元/MVC 测试及 9 项集成测试；收费 live 模型探针默认跳过。前端 40 项测试、lint 和 TypeScript/Vite 生产构建通过；保留原有大 chunk 告警。
- Chrome WebRTC：文字模式显示欢迎语；Enter 发送自我介绍问题，显示独立用户/助手字幕及完整模型回复。检测到 audio 元素未暂停时的非静音远端语音信号。
- Chrome WebRTC 打断：旧响应 `resp_YHkzQpNteUznBWDjtHQyl` 期间发送新问题，观察到 response.cancel、旧 response.done，再收到新 text.started / response.create；新回复“收到。”没有混入旧故事字幕。
- Chrome PCM：欢迎语和提问回复合计实际下发 32 个 audio.delta，显示完整独立字幕，没有 audio.append 或错误；观察到一个 avatar.gesture.started，证明自动动作实际开始，而非仅收到建议。
- Chrome PCM 打断：下一问约在旧响应开始 3.9 秒后发送，取消后收到新 text.started，新 responseId 正常结束，无错误。
- 自动测试覆盖无麦克风 offer、接收音轨、WebRTC 播放重建、PCM 音源停止、回执/拒绝/断连、IME 及字幕更新纯函数。

实际声学听感、字幕与语音逐字对齐、长时间稳定性，以及本轮未重新进行的真人语音输入，需要人工补充验收；不以协议事件和音频电平代替这些结论。测试会话在验收结束后关闭。

用户重启后又补强了重复提交和迟到完成事件的边界处理；最终使用前需重启后端加载完整代码并刷新页面。没有改动人设、音色或 API 密钥配置。
