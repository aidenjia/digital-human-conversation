package con.aidenjia.digitalhuman.conversation.api.bailian.webrtc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import con.aidenjia.digitalhuman.conversation.bailian.support.BailianRealtimeModels;
import con.aidenjia.digitalhuman.conversation.bailian.webrtc.BailianWebRtcDrainLifecycle;
import con.aidenjia.digitalhuman.conversation.bailian.webrtc.BailianWebRtcTicket;
import con.aidenjia.digitalhuman.conversation.bailian.webrtc.InMemoryBailianWebRtcTicketStore;
import con.aidenjia.digitalhuman.conversation.config.ApiSecurityProperties;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.socket.WebSocketHandler;

/** 控制连接用人设 query，模型以票据为准，空白人设不消费票据。 */
@ExtendWith(MockitoExtension.class)
class BailianRealtimeEventsHandshakeInterceptorTest {

    @Mock private JwtDecoder jwtDecoder;
    @Mock private BailianWebRtcDrainLifecycle drainLifecycle;
    @Mock private WebSocketHandler wsHandler;

    private InMemoryBailianWebRtcTicketStore tickets;
    private BailianRealtimeEventsHandshakeInterceptor interceptor;

    @BeforeEach
    void setUp() {
        tickets = new InMemoryBailianWebRtcTicketStore();
        ApiSecurityProperties apiSecurity = new ApiSecurityProperties();
        apiSecurity.setDemoTokenEnabled(false);
        interceptor =
                new BailianRealtimeEventsHandshakeInterceptor(
                        jwtDecoder, drainLifecycle, apiSecurity, tickets);
        when(drainLifecycle.isDraining()).thenReturn(false);
        tickets.save("relay-1", "user-42", BailianRealtimeModels.PLUS);
    }

    @Test
    @DisplayName("票据匹配且人设音色非空则接受并删除")
    void matchingTicketIsConsumed() {
        Map<String, Object> attrs =
                handshake("relay-1", "你叫黛黛", "longanqian", "user-42", BailianRealtimeModels.FLASH);

        assertThat(attrs).isNotNull();
        assertThat(attrs.get(BailianRealtimeEventsHandshakeInterceptor.ATTR_MODEL))
                .isEqualTo(BailianRealtimeModels.PLUS);
        assertThat(attrs.get(BailianRealtimeEventsHandshakeInterceptor.ATTR_PERSONA))
                .isEqualTo("你叫黛黛");
        assertThat(attrs.get(BailianRealtimeEventsHandshakeInterceptor.ATTR_VOICE))
                .isEqualTo("longanqian");
        assertThat(tickets.find("relay-1")).isEmpty();
    }

    @Test
    @DisplayName("同一票据第二次握手拒绝")
    void secondUseRejected() {
        assertThat(handshake("relay-1", "你叫黛黛", "longanqian", "user-42", null)).isNotNull();
        assertThat(handshake("relay-1", "你叫黛黛", "longanqian", "user-42", null)).isNull();
    }

    @Test
    @DisplayName("空白人设拒绝且不消费票据")
    void blankPersonaDoesNotConsume() {
        assertThat(handshake("relay-1", "  ", "longanqian", "user-42", null)).isNull();
        assertThat(tickets.find("relay-1"))
                .contains(new BailianWebRtcTicket("user-42", BailianRealtimeModels.PLUS));
    }

    @Test
    @DisplayName("用户不符拒绝且不消费票据")
    void foreignUserDoesNotConsume() {
        assertThat(handshake("relay-1", "你叫黛黛", "longanqian", "other", null)).isNull();
        assertThat(tickets.find("relay-1")).isPresent();
    }

    @Test
    @DisplayName("query model 不覆盖票据模型")
    void queryModelDoesNotOverrideTicket() {
        Map<String, Object> attrs =
                handshake("relay-1", "你叫黛黛", "longanqian", "user-42", BailianRealtimeModels.FLASH);
        assertThat(attrs.get(BailianRealtimeEventsHandshakeInterceptor.ATTR_MODEL))
                .isEqualTo(BailianRealtimeModels.PLUS);
    }

    private Map<String, Object> handshake(
            String relayId, String persona, String voice, String userId, String queryModel) {
        MockHttpServletRequest servlet =
                new MockHttpServletRequest("GET", "/ws/bailian/realtime-events");
        servlet.addHeader("Authorization", "Bearer good-token");
        servlet.setParameter("relayId", relayId);
        servlet.setParameter("persona", persona);
        servlet.setParameter("voice", voice);
        if (queryModel != null) {
            servlet.setParameter("model", queryModel);
        }
        Jwt jwt =
                Jwt.withTokenValue("good-token")
                        .header("alg", "none")
                        .subject(userId)
                        .claim("iss", "https://users.example")
                        .build();
        lenient().when(jwtDecoder.decode("good-token")).thenReturn(jwt);
        ServletServerHttpRequest request = new ServletServerHttpRequest(servlet);
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        ServletServerHttpResponse response = new ServletServerHttpResponse(servletResponse);
        Map<String, Object> attrs = new HashMap<>();
        boolean ok = interceptor.beforeHandshake(request, response, wsHandler, attrs);
        if (!ok) {
            assertThat(servletResponse.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            return null;
        }
        return attrs;
    }
}
