package con.aidenjia.digitalhuman.conversation.trtc.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConvoaiAsrLanguageMapperTest {

    @Test
    @DisplayName("zh-cn / zh-CN 映射为 zh-CN")
    void mapsChineseLocalesToZhCn() {
        assertEquals(
                ConvoaiAsrLanguageMapper.ASR_LANGUAGE_ZH_CN,
                ConvoaiAsrLanguageMapper.fromReplyLanguage("zh-cn"));
        assertEquals(
                ConvoaiAsrLanguageMapper.ASR_LANGUAGE_ZH_CN,
                ConvoaiAsrLanguageMapper.fromReplyLanguage("zh-CN"));
    }

    @Test
    @DisplayName("en-us / en-US 映射为 en-US")
    void mapsEnglishLocalesToEnUs() {
        assertEquals(
                ConvoaiAsrLanguageMapper.ASR_LANGUAGE_EN_US,
                ConvoaiAsrLanguageMapper.fromReplyLanguage("en-us"));
        assertEquals(
                ConvoaiAsrLanguageMapper.ASR_LANGUAGE_EN_US,
                ConvoaiAsrLanguageMapper.fromReplyLanguage("en-US"));
    }

    @Test
    @DisplayName("空白缺省为 zh-CN")
    void blankDefaultsToZhCn() {
        assertEquals(
                ConvoaiAsrLanguageMapper.ASR_LANGUAGE_ZH_CN,
                ConvoaiAsrLanguageMapper.fromReplyLanguage(null));
        assertEquals(
                ConvoaiAsrLanguageMapper.ASR_LANGUAGE_ZH_CN,
                ConvoaiAsrLanguageMapper.fromReplyLanguage("  "));
    }
}
