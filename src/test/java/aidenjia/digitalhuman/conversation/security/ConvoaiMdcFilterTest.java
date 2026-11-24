package con.aidenjia.digitalhuman.conversation.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import con.aidenjia.digitalhuman.conversation.trtc.support.ConvoaiLogContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ConvoaiMdcFilterTest {

    private static final String JOIN_BODY =
            """
            {"deviceId":"device-abc-123","digitalHumanType":"Unity"}
            """;

    private ConvoaiMdcFilter filter;

    @BeforeEach
    void setUp() {
        filter = new ConvoaiMdcFilter(new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        ConvoaiLogContext.clear();
    }

    @Test
    @DisplayName("POST join 在链路中写入 deviceId，结束后清理 MDC")
    void joinBindsDeviceIdDuringChain() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/convoai/agents");
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setContent(JOIN_BODY.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> deviceIdDuringChain = new AtomicReference<>();
        FilterChain chain =
                (req, res) -> deviceIdDuringChain.set(MDC.get(ConvoaiLogContext.MDC_DEVICE_ID));

        filter.doFilter(request, response, chain);

        assertEquals("device-abc-123", deviceIdDuringChain.get());
        assertNull(MDC.get(ConvoaiLogContext.MDC_DEVICE_ID));
    }

    @Test
    @DisplayName("DELETE leave 在链路中写入 agentId")
    void leaveBindsAgentIdDuringChain() throws ServletException, IOException {
        MockHttpServletRequest request =
                new MockHttpServletRequest("DELETE", "/api/convoai/agents");
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setContent("{\"agentId\":\"agent-xyz\"}".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> agentIdDuringChain = new AtomicReference<>();
        FilterChain chain =
                (req, res) -> agentIdDuringChain.set(MDC.get(ConvoaiLogContext.MDC_AGENT_ID));

        filter.doFilter(request, response, chain);

        assertEquals("agent-xyz", agentIdDuringChain.get());
        assertNull(MDC.get(ConvoaiLogContext.MDC_AGENT_ID));
    }

    @Test
    @DisplayName("非 ConvoAI 路径不经过 Filter")
    void skipsNonConvoaiPath() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/rtc/credentials");
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setContent("{\"deviceId\":\"device-1\"}".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> deviceIdDuringChain = new AtomicReference<>();
        FilterChain chain =
                (req, res) -> deviceIdDuringChain.set(MDC.get(ConvoaiLogContext.MDC_DEVICE_ID));

        filter.doFilter(request, response, chain);

        assertNull(deviceIdDuringChain.get());
    }
}
