package con.aidenjia.digitalhuman.conversation.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 声网对话式 AI 引擎（ConvoAI）REST 客户端配置。
 *
 * <p>所有敏感字段（密钥、Token、app key）应通过环境变量注入，勿硬编码。
 */
@ConfigurationProperties(prefix = "convoai")
public class ConvoaiProperties {

    /** 声网 App ID。 */
    private String appId;

    /** HTTP Basic Auth 客户 ID。 */
    private String username;

    /** HTTP Basic Auth 客户密钥。 */
    private String password;

    /** 域名区域（CN / AP / NA / EU），默认 CN。 */
    private String domainArea = "CN";

    /** 服务区域（CHINESE_MAINLAND / GLOBAL），默认 CHINESE_MAINLAND。 */
    private String serverRegion = "CHINESE_MAINLAND";

    /** 智能体在 RTC 频道内的用户 ID（String 或 Int 取决于 {@link #enableStringUid}）。 */
    private String agentRtcUid = "agent01";

    /** 是否使用 String 型 UID；与 {@link #agentRtcUid} 的取值方式保持一致。 */
    private boolean enableStringUid = true;

    /** RTC Token 鉴权与证书（用户 / 智能体进房 Token 动态生成）。 */
    private Rtc rtc = new Rtc();

    /** 空闲超时（秒），无其他用户时智能体自动离开。 */
    private int idleTimeout = 300;

    /** join 后内存中 {@code agentId → userId} 归属 TTL（秒）。默认 1 天，应不小于 {@link #idleTimeout}。 */
    private int agentOwnershipTtlSeconds = 86400;

    /** 智能体进房后主动播报的欢迎语，为空时不播报。 */
    private String greetingMessage = "";

    /** 安抚话术，为空时不写入 silence_config.content。 */
    private String comfortMessage = "";

    /** 安抚静默超时（ms）；写入 {@code parameters.silence_config.timeout_ms}。 */
    private int silenceTimeoutMs = 20000;

    /** 安抚行为：{@code speak} 或 {@code think}。 */
    private String silenceAction = "speak";

    // -------- LLM (custom, OpenAI 协议) --------
    private Llm llm = new Llm();

    /** join 成功后同步调用 agent session warm。 */
    private SessionWarm sessionWarm = new SessionWarm();

    // -------- TTS (声网 ConvoAI 内置 Tencent vendor) --------
    private Tts tts = new Tts();

    /** 打断策略（v2.6 {@code interruption}；默认开口打断）。 */
    private Interruption interruption = new Interruption();

    /** 对话轮次检测（v2.6 {@code turn_detection}）。 */
    private TurnDetection turnDetection = new TurnDetection();

    /** join 附加参数（{@code parameters}）。 */
    private Parameters parameters = new Parameters();

    /** ASR 配置；language 由音色推导，vendor 空则走凤鸣 ASR。 */
    private Asr asr = new Asr();

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDomainArea() {
        return domainArea;
    }

    public void setDomainArea(String domainArea) {
        this.domainArea = domainArea;
    }

    public String getServerRegion() {
        return serverRegion;
    }

    public void setServerRegion(String serverRegion) {
        this.serverRegion = serverRegion;
    }

    public String getAgentRtcUid() {
        return agentRtcUid;
    }

    public void setAgentRtcUid(String agentRtcUid) {
        this.agentRtcUid = agentRtcUid;
    }

    public Rtc getRtc() {
        return rtc;
    }

    public void setRtc(Rtc rtc) {
        this.rtc = rtc;
    }

    public boolean isEnableStringUid() {
        return enableStringUid;
    }

    public void setEnableStringUid(boolean enableStringUid) {
        this.enableStringUid = enableStringUid;
    }

    public int getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(int idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public int getAgentOwnershipTtlSeconds() {
        return agentOwnershipTtlSeconds;
    }

    public void setAgentOwnershipTtlSeconds(int agentOwnershipTtlSeconds) {
        this.agentOwnershipTtlSeconds = agentOwnershipTtlSeconds;
    }

    public String getGreetingMessage() {
        return greetingMessage;
    }

    public void setGreetingMessage(String greetingMessage) {
        this.greetingMessage = greetingMessage;
    }

    public String getComfortMessage() {
        return comfortMessage;
    }

    public void setComfortMessage(String comfortMessage) {
        this.comfortMessage = comfortMessage;
    }

    public int getSilenceTimeoutMs() {
        return silenceTimeoutMs;
    }

    public void setSilenceTimeoutMs(int silenceTimeoutMs) {
        this.silenceTimeoutMs = silenceTimeoutMs;
    }

    public String getSilenceAction() {
        return silenceAction;
    }

    public void setSilenceAction(String silenceAction) {
        this.silenceAction = silenceAction;
    }

    public Llm getLlm() {
        return llm;
    }

    public void setLlm(Llm llm) {
        this.llm = llm;
    }

    public SessionWarm getSessionWarm() {
        return sessionWarm;
    }

    public void setSessionWarm(SessionWarm sessionWarm) {
        this.sessionWarm = sessionWarm;
    }

    public Tts getTts() {
        return tts;
    }

    public void setTts(Tts tts) {
        this.tts = tts;
    }

    public Interruption getInterruption() {
        return interruption;
    }

    public void setInterruption(Interruption interruption) {
        this.interruption = interruption;
    }

    public TurnDetection getTurnDetection() {
        return turnDetection;
    }

    public void setTurnDetection(TurnDetection turnDetection) {
        this.turnDetection = turnDetection;
    }

    public Parameters getParameters() {
        return parameters;
    }

    public void setParameters(Parameters parameters) {
        this.parameters = parameters;
    }

    public Asr getAsr() {
        return asr;
    }

    public void setAsr(Asr asr) {
        this.asr = asr;
    }

    // 便捷 getter，Service 内部用
    public String getLlmUrl() {
        return llm == null ? null : llm.getUrl();
    }

    public String getLlmApiKey() {
        return llm == null ? null : llm.getApiKey();
    }

    public int getLlmMaxHistory() {
        return llm == null ? 1 : llm.getMaxHistory();
    }

    public String getTtsAppId() {
        return tts == null ? null : tts.getAppId();
    }

    public Integer getTtsVoiceType() {
        return tts == null ? null : tts.getVoiceType();
    }

    public Integer getTtsVolume() {
        return tts == null ? null : tts.getVolume();
    }

    public Integer getTtsSpeed() {
        return tts == null ? null : tts.getSpeed();
    }

    public String getTtsEmotionCategory() {
        return tts == null ? null : tts.getEmotionCategory();
    }

    public Integer getTtsEmotionIntensity() {
        return tts == null ? null : tts.getEmotionIntensity();
    }

    public int getTtsSampleRate() {
        return tts == null ? 24000 : tts.getSampleRate();
    }

    public static class Llm {
        private String url;
        private String apiKey;

        /** 声网 LLM 短期记忆条目数，默认 1（长历史由 agent 侧维护）。 */
        private int maxHistory = 1;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public int getMaxHistory() {
            return maxHistory;
        }

        public void setMaxHistory(int maxHistory) {
            this.maxHistory = maxHistory;
        }
    }

    /** Agent {@code POST /v1/sessions/warm}：join 成功后同步 best-effort 预热。 */
    public static class SessionWarm {
        /** 是否在 Agora join 成功后调用 warm；默认开启。 */
        private boolean enabled = true;

        /** HTTP 调用超时（毫秒）；默认 5000。 */
        private long timeoutMs = 5000;

        /**
         * 显式 warm URL；为空时从 {@code convoai.llm.url} 将 {@code /v1/chat/completions} 替换为 {@code
         * /v1/sessions/warm}。
         */
        private String url;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    public static class Rtc {
        /** 声网 App 证书，用于动态生成 RTC Token。 */
        private String appCertificate;

        /** 是否开启 Token 鉴权；关闭时 join 响应中 {@code token} 为 {@code null}。 */
        private boolean tokenAuthEnabled = true;

        /** RTC Token 有效期（秒），默认 24h。 */
        private int tokenExpireSeconds = 86400;

        public String getAppCertificate() {
            return appCertificate;
        }

        public void setAppCertificate(String appCertificate) {
            this.appCertificate = appCertificate;
        }

        public boolean isTokenAuthEnabled() {
            return tokenAuthEnabled;
        }

        public void setTokenAuthEnabled(boolean tokenAuthEnabled) {
            this.tokenAuthEnabled = tokenAuthEnabled;
        }

        public int getTokenExpireSeconds() {
            return tokenExpireSeconds;
        }

        public void setTokenExpireSeconds(int tokenExpireSeconds) {
            this.tokenExpireSeconds = tokenExpireSeconds;
        }
    }

    public static class Tts {
        private String appId;
        private Integer voiceType = 602002;
        private Integer volume = 5;
        private Integer speed = 0;
        private String emotionCategory = "happy";
        private Integer emotionIntensity = 100;
        private int sampleRate = 24000;

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public Integer getVoiceType() {
            return voiceType;
        }

        public void setVoiceType(Integer voiceType) {
            this.voiceType = voiceType;
        }

        public Integer getVolume() {
            return volume;
        }

        public void setVolume(Integer volume) {
            this.volume = volume;
        }

        public Integer getSpeed() {
            return speed;
        }

        public void setSpeed(Integer speed) {
            this.speed = speed;
        }

        public String getEmotionCategory() {
            return emotionCategory;
        }

        public void setEmotionCategory(String emotionCategory) {
            this.emotionCategory = emotionCategory;
        }

        public Integer getEmotionIntensity() {
            return emotionIntensity;
        }

        public void setEmotionIntensity(Integer emotionIntensity) {
            this.emotionIntensity = emotionIntensity;
        }

        public int getSampleRate() {
            return sampleRate;
        }

        public void setSampleRate(int sampleRate) {
            this.sampleRate = sampleRate;
        }
    }

    public static class Interruption {
        private boolean enable = true;
        private String mode = "start_of_speech";
        private List<String> triggerKeywords = defaultTriggerKeywords();

        public boolean isEnable() {
            return enable;
        }

        public void setEnable(boolean enable) {
            this.enable = enable;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public List<String> getTriggerKeywords() {
            return triggerKeywords;
        }

        public void setTriggerKeywords(List<String> triggerKeywords) {
            this.triggerKeywords = triggerKeywords != null ? triggerKeywords : new ArrayList<>();
        }

        private static List<String> defaultTriggerKeywords() {
            return List.of(
                    "停止", "停", "停下", "停一下", "别说了", "不要说了", "暂停", "打断", "等一下", "稍等", "等等", "先等等",
                    "慢着", "安静", "够了");
        }
    }

    public static class TurnDetection {
        private float speechThreshold = 0.5F;
        private int interruptDurationMs = 160;
        private int speakingInterruptDurationMs = 280;
        private int prefixPaddingMs = 800;
        private int silenceDurationMs = 320;

        public float getSpeechThreshold() {
            return speechThreshold;
        }

        public void setSpeechThreshold(float speechThreshold) {
            this.speechThreshold = speechThreshold;
        }

        public int getInterruptDurationMs() {
            return interruptDurationMs;
        }

        public void setInterruptDurationMs(int interruptDurationMs) {
            this.interruptDurationMs = interruptDurationMs;
        }

        public int getSpeakingInterruptDurationMs() {
            return speakingInterruptDurationMs;
        }

        public void setSpeakingInterruptDurationMs(int speakingInterruptDurationMs) {
            this.speakingInterruptDurationMs = speakingInterruptDurationMs;
        }

        public int getPrefixPaddingMs() {
            return prefixPaddingMs;
        }

        public void setPrefixPaddingMs(int prefixPaddingMs) {
            this.prefixPaddingMs = prefixPaddingMs;
        }

        public int getSilenceDurationMs() {
            return silenceDurationMs;
        }

        public void setSilenceDurationMs(int silenceDurationMs) {
            this.silenceDurationMs = silenceDurationMs;
        }
    }

    public static class Parameters {
        /** AI 对话场景音频优化，见声网 {@code parameters.audio_scenario}。 */
        private String audioScenario = "aiserver";

        public String getAudioScenario() {
            return audioScenario;
        }

        public void setAudioScenario(String audioScenario) {
            this.audioScenario = audioScenario;
        }
    }

    public static class Asr {
        /** ASR 供应商；空或未设时仅传 language（由音色推导），上游默认凤鸣。 */
        private String vendor = "";

        public String getVendor() {
            return vendor;
        }

        public void setVendor(String vendor) {
            this.vendor = vendor;
        }
    }
}
