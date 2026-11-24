## 1. Persistence & config

- [x] 1.1 扩展 `CompanionCustomVoice` 实体/Mapper：补齐生产字段映射（含 `trainingTextId`、`providerTaskId`、`corpusUri`、`gender`、`sourceLanguage`、`domain`、`contactPhone`、`costStarCores`、`failReason` 等建单/训练所需列）
- [x] 1.2 新增语言/领域合法值常量或枚举 + 腾讯 Domain/VoiceGender/VoiceLanguage 映射工具
- [x] 1.3 新增腾讯 VRS `*Properties`（密钥外置）并接入 `application-*.yml` / 本地 ENC 约定；CD 占位说明写入设计备注或 README
- [x] 1.4 Redis AudioId 缓存封装（key 绑 customVoiceId + TTL）（可选；同请求 Detect→Create 可后置） — **本期不做**：同请求 Detect→Create，不依赖 Redis

## 2. Tencent VRS client

- [x] 2.1 Spike 结论（已钉死）：**一句话 TaskType=5**；Detect 仅 AudioData；落库数字 VoiceType（忽略 FastVoiceType）
- [x] 2.2 **删除 Mock**；仅保留 Real 路径（禁止假联调）
- [x] 2.2b 实现 **Real** Client（官方 SDK 优先）：四接口真实调用；缺密钥 → ConfigurationException
- [x] 2.3 服务层单测：Mockito 注入 `TencentVrsClient`（不依赖 Mock 实现类）
- [x] 2.4 Real Client 映射单测（Status→state、VoiceType→providerVoiceRef；不依赖外网）

## 3. Custom voice production APIs

- [x] 3.1 新建 `api.customvoice` Controller + DTO；Security `authenticated()`
- [x] 3.2 `GET /training-text`：不建单；经真实 VRS 拉文案
- [x] 3.3 `POST /api/custom-voices` 提交：建单 `custom_00N` + 检测 + 训练（`pay_status=2`）
- [x] 3.3b 提交：建单后用音频调 Detect/Create；`corpus_uri` 已由上传写入；codec/sampleRate 下传
- [x] 3.3c **本期必做**：后端语料上传 → PutObject → 返回 `corpusUri`；对象键 `custom-voice/{userId}/{uuid}.ext`
- [x] 3.3d 语料临时读 URL — **已撤销**：不做 `corpus-url`；前端本地播录音，合成试听另议
- [x] 3.4 `GET /{businessCode}` 轮询回写终态；归属校验 `user_id=sub`

## 4. Tests & docs

- [x] 4.1 API/服务测试：缺音频、检测失败不训练、轮询成功、未找到 404（Mockito VRS）
- [x] 4.2 `mvn spotless:apply` 与 `mvn verify`
- [x] 4.3 更新客户端/联调文档：桌面 `定制音色-现状说明.md` + README 定制音色小节 + design §7（过期/额度/配置）
