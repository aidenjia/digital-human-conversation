package con.aidenjia.digitalhuman.conversation.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/** 演示旁路白名单包含 WebRTC 信令，且只放行 POST。 */
class DemoBearerAuthenticationFilterPathTest {

    @Test
    @DisplayName("POST /api/bailian/realtime/webrtc 走演示旁路")
    void webrtcPostIsBypass() {
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/api/bailian/realtime/webrtc");
        assertThat(DemoBearerAuthenticationFilter.matchesDemoBypassPath(request)).isTrue();
    }

    @Test
    @DisplayName("GET 信令路径不走演示旁路")
    void webrtcGetIsNotBypass() {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/bailian/realtime/webrtc");
        assertThat(DemoBearerAuthenticationFilter.matchesDemoBypassPath(request)).isFalse();
    }
}
