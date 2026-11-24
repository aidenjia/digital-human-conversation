package con.aidenjia.digitalhuman.conversation.trtc.support;

import con.aidenjia.digitalhuman.conversation.config.ConvoaiProperties;
import con.aidenjia.digitalhuman.conversation.config.TencentCloudCredentialsProperties;
import con.aidenjia.digitalhuman.conversation.exception.ConfigurationException;
import con.aidenjia.digitalhuman.conversation.trtc.command.JoinConvoaiCommand;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** 组装声网 ConvoAI join REST body（v2.6 turn_detection / interruption）。 */
@Component
public class ConvoaiJoinBodyAssembler {

    private final TencentCloudCredentialsProperties credentials;

    public ConvoaiJoinBodyAssembler(TencentCloudCredentialsProperties credentials) {
        this.credentials = credentials;
    }

    public Map<String, Object> build(
            ConvoaiProperties props,
            JoinConvoaiCommand command,
            String channel,
            String userId,
            String agentToken,
            JoinSessionProfile profile) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", props.getAppId() + ":" + channel);

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("channel", channel);
        properties.put("token", agentToken);
        properties.put("agent_rtc_uid", props.getAgentRtcUid());
        properties.put("remote_rtc_uids", List.of(userId));
        properties.put("enable_string_uid", props.isEnableStringUid());
        properties.put("idle_timeout", props.getIdleTimeout());

        Map<String, Object> llmParams = new LinkedHashMap<>();
        llmParams.put("clientUserId", userId);
        llmParams.put("deviceId", command.deviceId());
        llmParams.put("digitalHumanType", command.digitalHumanType().name());
        llmParams.put("model", "custom");
        llmParams.put("stream", true);
        llmParams.put("conversationId", channel);
        llmParams.put("replyLanguage", profile.replyLanguage());
        putTimezoneIfPresent(llmParams, command.timezone());

        Map<String, Object> llm = new LinkedHashMap<>();
        llm.put("url", props.getLlmUrl());
        llm.put("api_key", props.getLlmApiKey());
        llm.put("vendor", "custom");
        llm.put("max_history", props.getLlmMaxHistory());
        llm.put("params", llmParams);
        profile.greetingMessage(props).ifPresent(g -> llm.put("greeting_message", g));
        properties.put("llm", llm);

        properties.put("tts", buildTtsMap(props, profile));
        properties.put("asr", buildAsrMap(props, profile));
        properties.put("parameters", buildParametersMap(props, profile));
        properties.put("turn_detection", buildTurnDetection(props));
        properties.put("interruption", buildInterruption(props));

        body.put("properties", properties);
        return body;
    }

    private Map<String, Object> buildTtsMap(ConvoaiProperties props, JoinSessionProfile profile) {
        if (isBlank(props.getTtsAppId()) || !credentials.isConfigured()) {
            throw new ConfigurationException(
                    "ConvoAI Tencent TTS credentials (app-id/secret-id/secret-key) are not configured");
        }
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("app_id", props.getTtsAppId());
        params.put("secret_id", credentials.getSecretId());
        params.put("secret_key", credentials.getSecretKey());
        profile.putTtsVoiceAndScale(params, props);
        params.put("emotion_category", props.getTtsEmotionCategory());
        params.put("emotion_intensity", props.getTtsEmotionIntensity());
        params.put("sample_rate", props.getTtsSampleRate());

        Map<String, Object> tts = new LinkedHashMap<>();
        tts.put("vendor", "tencent");
        tts.put("params", params);
        return tts;
    }

    private static Map<String, Object> buildAsrMap(
            ConvoaiProperties props, JoinSessionProfile profile) {
        ConvoaiProperties.Asr asr = props.getAsr();
        Map<String, Object> asrMap = new LinkedHashMap<>();
        asrMap.put("language", ConvoaiAsrLanguageMapper.fromReplyLanguage(profile.replyLanguage()));

        if (!isBlank(asr.getVendor()) && !"fengming".equalsIgnoreCase(asr.getVendor())) {
            asrMap.put("vendor", asr.getVendor());
        }
        return asrMap;
    }

    private static Map<String, Object> buildParametersMap(
            ConvoaiProperties props, JoinSessionProfile profile) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        String audioScenario = props.getParameters().getAudioScenario();
        if (!isBlank(audioScenario)) {
            parameters.put("audio_scenario", audioScenario);
        }
        putSilenceConfig(parameters, props, profile);
        return parameters;
    }

    private static void putSilenceConfig(
            Map<String, Object> parameters, ConvoaiProperties props, JoinSessionProfile profile) {
        Optional<String> comfort = profile.comfortText(props);
        if (comfort.isEmpty() || props.getSilenceTimeoutMs() <= 0) {
            return;
        }
        Map<String, Object> silenceConfig = new LinkedHashMap<>();
        silenceConfig.put("timeout_ms", props.getSilenceTimeoutMs());
        silenceConfig.put(
                "action", isBlank(props.getSilenceAction()) ? "speak" : props.getSilenceAction());
        silenceConfig.put("content", comfort.get());
        parameters.put("silence_config", silenceConfig);
    }

    private static Map<String, Object> buildTurnDetection(ConvoaiProperties props) {
        ConvoaiProperties.TurnDetection td = props.getTurnDetection();

        Map<String, Object> vadConfigSos = new LinkedHashMap<>();
        vadConfigSos.put("interrupt_duration_ms", td.getInterruptDurationMs());
        vadConfigSos.put("speaking_interrupt_duration_ms", td.getSpeakingInterruptDurationMs());
        vadConfigSos.put("prefix_padding_ms", td.getPrefixPaddingMs());

        Map<String, Object> startOfSpeech = new LinkedHashMap<>();
        startOfSpeech.put("mode", "vad");
        startOfSpeech.put("vad_config", vadConfigSos);

        Map<String, Object> vadConfigEos = new LinkedHashMap<>();
        vadConfigEos.put("silence_duration_ms", td.getSilenceDurationMs());

        Map<String, Object> endOfSpeech = new LinkedHashMap<>();
        endOfSpeech.put("mode", "vad");
        endOfSpeech.put("vad_config", vadConfigEos);

        Map<String, Object> turnConfig = new LinkedHashMap<>();
        turnConfig.put("speech_threshold", td.getSpeechThreshold());
        turnConfig.put("start_of_speech", startOfSpeech);
        turnConfig.put("end_of_speech", endOfSpeech);

        Map<String, Object> turnDetection = new LinkedHashMap<>();
        turnDetection.put("mode", "default");
        turnDetection.put("config", turnConfig);
        return turnDetection;
    }

    private static Map<String, Object> buildInterruption(ConvoaiProperties props) {
        ConvoaiProperties.Interruption cfg = props.getInterruption();

        Map<String, Object> interruption = new LinkedHashMap<>();
        interruption.put("enable", cfg.isEnable());
        interruption.put("mode", cfg.getMode());
        if ("keywords".equalsIgnoreCase(cfg.getMode())) {
            Map<String, Object> keywordsConfig = new LinkedHashMap<>();
            keywordsConfig.put("trigger_keywords", List.copyOf(cfg.getTriggerKeywords()));
            interruption.put("keywords_config", keywordsConfig);
        }
        return interruption;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static void putTimezoneIfPresent(Map<String, Object> llmParams, String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return;
        }
        llmParams.put("timezone", timezone.trim());
    }
}
