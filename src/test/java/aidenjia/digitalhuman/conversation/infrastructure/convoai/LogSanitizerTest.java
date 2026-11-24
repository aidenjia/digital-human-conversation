package con.aidenjia.digitalhuman.conversation.infrastructure.convoai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LogSanitizerTest {

    @Test
    @DisplayName("truncate：null 与空串返回空")
    void truncateNullOrEmpty() {
        assertEquals("", LogSanitizer.truncate(null));
        assertEquals("", LogSanitizer.truncate(""));
    }

    @Test
    @DisplayName("truncate：短于上限原样返回")
    void truncateShortValue() {
        assertEquals("ok", LogSanitizer.truncate("ok"));
    }

    @Test
    @DisplayName("truncate：超过 500 字符截断并加省略号")
    void truncateLongValue() {
        String longValue = "x".repeat(LogSanitizer.MAX_LOG_BODY_LENGTH + 10);
        String truncated = LogSanitizer.truncate(longValue);
        assertEquals(LogSanitizer.MAX_LOG_BODY_LENGTH + 3, truncated.length());
        assertFalse(truncated.endsWith("x"));
        assertEquals("...", truncated.substring(truncated.length() - 3));
    }
}
