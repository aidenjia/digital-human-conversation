package con.aidenjia.digitalhuman.conversation.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthFailureResponsesTest {

    @Test
    @DisplayName("英文鉴权失败文案对齐指南")
    void englishMessages() {
        assertThat(
                        AuthFailureResponses.messageForCulture(
                                "en", AuthFailureResponses.CODE_UNAUTHENTICATED))
                .isEqualTo("Cannot authenticate");
        assertThat(
                        AuthFailureResponses.messageForCulture(
                                "en", AuthFailureResponses.CODE_INVALID_TOKEN))
                .isEqualTo("Token is invalid");
    }

    @Test
    @DisplayName("中文鉴权失败文案对齐指南")
    void chineseMessages() {
        assertThat(
                        AuthFailureResponses.messageForCulture(
                                "zh", AuthFailureResponses.CODE_UNAUTHENTICATED))
                .isEqualTo("无法鉴权访问");
        assertThat(
                        AuthFailureResponses.messageForCulture(
                                "zh", AuthFailureResponses.CODE_INVALID_TOKEN))
                .isEqualTo("访问令牌已失效");
    }
}
