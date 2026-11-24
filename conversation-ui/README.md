# 语音会话演示

使用固定 Token 登录后选择百炼 WebRTC 或声网 ConvoAI。百炼页面支持文字或语音提问、语音与字幕回复、VRM 数字人舞台；声网页面选择会话类型后直接开始通话，语音参数使用后端配置。

## 本地运行

```powershell
npm ci
npm run dev
```

根据终端地址访问，默认端口 5173。麦克风需要 HTTPS 或 localhost，文字模式可直接输入。开发模式默认连接 `http://localhost:8080`。登录页接受 Token 本身或 `Bearer <Token>`，HTTP 和 WebSocket 请求由后端校验。退出登录会清除本地凭据。完整接口和控制事件见 [前端对接](../docs/frontend-realtime-voice-feishu.md)。

使用后端演示固定 Token 时，需要开启 `api.security.demo-token-enabled`，并将 `api.security.demo-token` 配成输入的同一值。百炼 API Key 仍只保存在后端，不能作为这里的登录 Token。

## 部署

使用实际的后端地址构建。Vite 会把 `VITE_API_BASE` 写入浏览器产物，该变量只填写 URL。

```powershell
$env:VITE_API_BASE = 'https://api.example.com'
npm run build
```

静态产物在 `dist/`。部署时替换示例域名，并确保代理支持 `/ws/bailian/realtime-events` 的 WebSocket Upgrade。

## 验证

```powershell
node --test tests/*.test.ts
npm run lint
npm run build
```

涉及真实骨骼的测试需先准备本地默认 VRM，见 [Avatar 说明](AVATAR.md)。全新检出可先执行 `npm ci`。

百炼音频通过 WebRTC RTP 播放，控制事件通过 `/ws/bailian/realtime-events` 编排；声网通过 ConvoAI join/leave 和 RTC SDK 通话。页面不提供用户配置保存、音色目录、复制、试听或在线动作生成。

## 日志

默认调试日志关闭。浏览器控制台中设置后刷新：

```javascript
localStorage.debug = 'AGORA_AI_VOICE_ASSISTANT*'
location.reload()
```

关闭时移除 `debug` 键。日志中不要记录访问凭据、SDP 或人设正文。
