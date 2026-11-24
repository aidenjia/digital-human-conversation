# Voice preview by language Implementation Plan

> **For agentic workers:** Implement task-by-task with TDD. Checkboxes in `tasks.md` are the source of progress.

**Goal:** Preset voice preview synthesizes Chinese or English PCM from catalog `language` (`zh-cn` / `en-us`). Custom production and custom preview language branching stay unchanged.

**Architecture:** Resolver attaches `language` for presets → Service picks sample text + `PrimaryLanguage` → Tencent TextToVoice. Cache hit requires matching `sample_text`; mismatch UPDATE.

**Tech Stack:** Spring Boot 3.5, existing Tencent TTS SDK, Postgres `voice_preview_audio` (no DDL).

---

### Task 1: Config and TTS client

**Files:**
- Modify: `VoicePreviewProperties.java`, `application.yml`
- Modify: `TencentTtsClient.java`, `RealTencentTtsClient.java`, `RealTencentTtsClientTest.java`

- [ ] **Step 1:** Add `sampleTextEn` (default English sentence) + yml `sample-text-en`
- [ ] **Step 2:** `textToPcm(..., int primaryLanguage)` and `req.setPrimaryLanguage`

### Task 2: Resolver + mapper + service

**Files:**
- Modify: `ResolvedVoiceRef.java`, `VoiceCatalogResolver.java`
- Create: `VoicePreviewLanguageMapper.java` (or equivalent in `voicepreview.support`)
- Modify: `VoicePreviewService.java`

- [ ] **Step 1:** Preset resolve copies `language`; custom leaves null
- [ ] **Step 2:** Map `en*` → English text + PrimaryLanguage 2, else Chinese + 1
- [ ] **Step 3:** Stale cache (`sample_text` mismatch) resynthesize + UPDATE

### Task 3: Tests, README, verify

**Files:**
- Modify: `VoicePreviewServiceTest.java`, `VoiceCatalogResolverTest.java` as needed
- Modify: `README.md`

- [ ] **Step 1:** Tests for zh-cn / en-us / custom / stale cache / fresh cache
- [ ] **Step 2:** README language rule
- [ ] **Step 3:** `mvn spotless:apply` && `mvn verify`
