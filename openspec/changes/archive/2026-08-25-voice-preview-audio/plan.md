# Voice Preview Audio Implementation Plan

> **For agentic workers:** Use superpowers:subagent-driven-development
> to implement this plan task-by-task.

**Goal:** 交付 `GET /api/voice-previews`：按 `businessCode` 解析雪花 id，读写已有 `voice_preview_audio`，未命中则 TextToVoice 落库并返回 base64 PCM。

**Architecture:** 薄 `api.voicepreview` Controller → `voicepreview` Service（目录解析 + 缓存编排）→ persistence Mapper + `infrastructure.tts` Client。表结构不改；密钥走 `tencent.credentials`；文案走 `digital-human.voice-preview.*`。

**Tech Stack:** Java 21、Spring Boot 3.5、MyBatis-Plus、腾讯云 TTS（TextToVoice）、Postgres `bytea`、JUnit/MockMvc。

---

## Task 1: Persistence & config

- [ ] **Step 1:** 写失败测试占位或 Entity 映射冒烟：表字段 `voice_ref_id`/`pcm_data`/`sample_text`/`owner_user_id`
- [ ] **Step 2:** 实现 `VoicePreviewAudio` + `VoicePreviewAudioMapper`（按三列查；插入）
- [ ] **Step 3:** `VoicePreviewProperties` + yml 默认文案「树影婆娑，阳光正好。让声音为你打开一段温柔时光。」与 `sample-rate: 16000`
- [ ] **Step 4:** 可选复制 DDL 到 `docs/sql/voice-preview-audio.sql`

## Task 2: TTS client

- [ ] **Step 1:** 定义 `TencentTtsClient.textToPcm(text, voiceType, sampleRate)` 接口
- [ ] **Step 2:** Real 实现调用 TextToVoice（`Codec=pcm`）；解码 `Audio` base64 → `byte[]`
- [ ] **Step 3:** Mock 测试：成功返回非空 bytes；上游错误抛可映射异常

## Task 3: Resolver + Service

- [ ] **Step 1:** 红测：未知 code → 404；dimension+custom → 400；定制非本人 → 403/404
- [ ] **Step 2:** 实现目录解析（companion/dimension preset + companion custom）
- [ ] **Step 3:** 红测：缓存命中不调 TTS；未命中调 TTS 并 insert
- [ ] **Step 4:** 实现 `VoicePreviewService`；唯一冲突 catch 后 re-select
- [ ] **Step 5:** 绿测跑通 Service 套件

## Task 4: API & security

- [ ] **Step 1:** Controller + `VoicePreviewResponse`（`audioBase64`/`sampleRate`/`codec`）
- [ ] **Step 2:** Security 配置纳入 `/api/voice-previews/**`
- [ ] **Step 3:** MockMvc：缺 token 401；合法 token + mock service 200
- [ ] **Step 4:** README +（实现后）同步活规格路径列表

## Task 5: Verify

- [ ] **Step 1:** `mvn spotless:apply`
- [ ] **Step 2:** `mvn verify`
