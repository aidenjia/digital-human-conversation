package con.aidenjia.digitalhuman.conversation.trtc.support;

import java.util.Locale;

/** 音色语言（replyLanguage）→ 声网 ConvoAI {@code properties.asr.language} 映射。 */
public final class ConvoaiAsrLanguageMapper {

    public static final String ASR_LANGUAGE_ZH_CN = "zh-CN";
    public static final String ASR_LANGUAGE_EN_US = "en-US";

    private ConvoaiAsrLanguageMapper() {}

    /**
     * 将音色 / LLM 侧 locale（如 {@code zh-cn}、{@code en-us}、{@code zh-CN}）映射为声网 ASR 枚举。
     *
     * <p>声网仅支持 {@code zh-CN}（中文为主，可夹杂英文）与 {@code en-US}（英语为主）。
     */
    public static String fromReplyLanguage(String replyLanguage) {
        if (replyLanguage == null || replyLanguage.isBlank()) {
            return ASR_LANGUAGE_ZH_CN;
        }
        String normalized = replyLanguage.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("en")) {
            return ASR_LANGUAGE_EN_US;
        }
        return ASR_LANGUAGE_ZH_CN;
    }
}
