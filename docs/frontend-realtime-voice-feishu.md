# 语音会话前端对接

当前支持声网 ConvoAI 与百炼 WebRTC。HTTP 响应统一为 `ApiResponse`，业务数据读取 `data`。页面仅支持输入固定 Token，保存后用于 HTTP Bearer 和 WebSocket 鉴权，不调用验证码或 Users 登录接口；Token 是否有效由后端校验。禁止在日志中记录 token、人设正文或 SDP。

## 声网

1. POST `/api/convoai/agents`，传 `deviceId`、`digitalHumanType`（UE5/Unity/MMD）和可选 `timezone`。
2. 读取 `appId/channel/token/userId/agentId`，声网 SDK 进房、发布麦克风、订阅远端音频。
3. 开关麦克风只控制当前通话采集。
4. DELETE 同一接口，body 为 `{"agentId":"..."}`，然后离房并释放麦克风。
5. join 不读取角色目录或已保存设置。会话音色、语速、音量使用后端配置。

## 百炼 WebRTC 信令

POST `/api/bailian/realtime/webrtc`，Header 为 `Authorization: Bearer <Users JWT>`：

```json
{"offerSdp":"<browser offer>","model":"qwen-audio-3.0-realtime-flash"}
```

模型允许 flash/plus；省略使用 flash。响应 `data`：

```json
{"answerSdp":"<upstream answer>","relayId":"<one-time ticket>","model":"qwen-audio-3.0-realtime-flash"}
```

`relayId` 60 秒有效，只绑定当前用户与已选择模型。控制握手先验证必填参数与用户再消费票据。

## 建连顺序

1. 语音模式请求麦克风；文字模式创建接收音频的 transceiver，不请求麦克风。
2. 创建 PeerConnection 与 Offer 中的数据通道 `oai-events`，收集 ICE 后发信令 POST。
3. 先连接控制 WS，再设置远端 SDP：

```text
/ws/bailian/realtime-events?access_token=<JWT>&relayId=<ticket>&persona=<URL encoded persona>&voice=<Bailian voice>&enableAction=true
```

`persona`、`voice` trim 后必须非空。`enableAction=true|1` 开启动作工具，其他值关闭。query 中的 model 不能覆盖票据模型。Header Bearer 客户端也受支持。

4. 百炼在服务端创建的数据通道 `txt` 上推事件。客户端缓存未就绪命令，将百炼 JSON 转发到控制 WS，过滤 `response.audio.delta`。
5. 控制端返回 `{"type":"downstream.command","event":{...}}`，将 `event` 写回 `txt`。
6. 远端 RTP 音频走 `ontrack` 与 Audio 元素；控制 WS 不承载音频。不要依据数据通道音频 delta 另建播放链路。
7. 控制失败则释放 PeerConnection；新的连接必须重新申请票据。

## 控制事件与交互

保留 `session.ready`、`speech.started`、`speech.stopped`、`transcript.user`、`transcript.assistant`、`avatar.response`、`avatar.expression`、`avatar.decision`、`action`、`error`、`pong`、`server.draining`。事件处理契约以 `bailianRealtimeEventsWs.ts`、`BailianRealtimeEventsSession` 为准。

- 文本提交使用 `text.submit`，通过 requestId 匹配接受/拒绝，避免重复发送；接受后由语音和字幕回复。
- 字幕按 responseId 隔离，React 状态 updater 保持纯函数。
- 打断时停止旧播放与动作，旧 response 事件不得恢复旧表情或字幕。
- 固定欢迎语只语音和字幕，不计自动挥手冷却；动作真正开始时客户端发送 `avatar.gesture.started` 回执。
- 自动动作使用语义意图 `none/greet/agree/explain`；前端映射为不动作、挥手、点头或解释动作。`explain` 只表示助手正在展开讲解，不因用户提到“解释”就触发。
- `avatar.decision` 包含 `responseId/cueId/segmentId/intent/emotion/expiresInMs`，当前后端只发 `opening` 片段。前端等待实际音频活动后再开始动作；解释素材预加载，不就绪、过期、忙碌超时或被打断时跳过。
- 自动动作沿用全局 8 秒、同意图 30 秒冷却；回执携带 `responseId/cueId/intent/motionId`，只有真正开始播放才记录后端冷却。前后端新增 `explain` 需同步更新，旧前端会忽略未知意图。
- 控制连接定期 ping，空闲超时关闭；`server.draining` 表示服务下线，结束连接并新建会话，不跨实例续接旧控制状态。
- 百炼 WebRTC 不调用声网进房凭据 API。
- 朗读、念诗、讲故事等内容由实时语音模型直接回应，不映射成身体动作。已移除无前端执行能力的 `sing` 工具命令及其固定唱歌确认语；口播不包含“此处为模拟”等舞台说明。更新后端后须新建会话，以重新下发规则和工具 Schema。

## 前端配置与验证

`VITE_API_BASE` 指向会话后端，不再使用 `VITE_AUTH_BASE` 与 `VITE_CLIENT_ID`。开发环境在 `conversation-ui` 先执行 `npm ci`，再执行 `npm run dev`；生产执行 `npm run build`。使用演示固定 Token 时，后端须开启演示 Token 开关并配置匹配值，HTTP 接口白名单和 WebRTC 控制握手规则保持不变。

页面模式只允许 agora/webrtc；历史存储值回到模式选择。数字人模型与动作本地加载，不需要动作生成服务。

验证覆盖语音输入、文字无麦克风、实际 RTP 出声、双向字幕、长回答打断、旧回复隔离、动作开始回执、结束及重新建连。自动化命令为 `node --test tests/*.test.ts`、`npm run lint`、`npm run build`；真实通话需另外验收。
