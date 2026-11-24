package con.aidenjia.digitalhuman.conversation.api.bailian.webrtc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import con.aidenjia.digitalhuman.conversation.bailian.support.BailianRealtimeModels;
import con.aidenjia.digitalhuman.conversation.bailian.webrtc.BailianWebRtcDrainLifecycle;
import con.aidenjia.digitalhuman.conversation.bailian.webrtc.InMemoryBailianWebRtcTicketStore;
import con.aidenjia.digitalhuman.conversation.config.ApiSecurityProperties;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.socket.WebSocketHandler;

class BailianRealtimeEventsAuthenticationTest {
    final JwtDecoder decoder = mock(JwtDecoder.class);
    final BailianWebRtcDrainLifecycle drain = mock(BailianWebRtcDrainLifecycle.class);
    final ApiSecurityProperties security = new ApiSecurityProperties();
    final InMemoryBailianWebRtcTicketStore tickets = new InMemoryBailianWebRtcTicketStore();
    final MockHttpServletRequest request = new MockHttpServletRequest();
    final MockHttpServletResponse response = new MockHttpServletResponse();
    final HashMap<String, Object> attributes = new HashMap<>();

    BailianRealtimeEventsAuthenticationTest() {
        security.setDemoTokenEnabled(false);
        request.setParameter("relayId", "ticket");
        request.setParameter("persona", "persona");
        request.setParameter("voice", "voice");
        tickets.save("ticket", "user", BailianRealtimeModels.PLUS);
    }

    boolean handshake() {
        return new BailianRealtimeEventsHandshakeInterceptor(decoder, drain, security, tickets)
                .beforeHandshake(
                        new ServletServerHttpRequest(request),
                        new ServletServerHttpResponse(response),
                        mock(WebSocketHandler.class),
                        attributes);
    }

    @Test
    void missingTokenRejectsWithoutConsumingTicket() {
        assertThat(handshake()).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(tickets.find("ticket")).isPresent();
        verifyNoInteractions(decoder);
    }

    @Test
    void invalidJwtRejectsWithoutConsumingTicket() {
        request.addHeader("Authorization", "Bearer bad");
        when(decoder.decode("bad")).thenThrow(new JwtException("invalid"));
        assertThat(handshake()).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(tickets.find("ticket")).isPresent();
    }

    @Test
    void drainingRejectsBeforeAuthenticationAndTicketConsumption() {
        when(drain.isDraining()).thenReturn(true);
        assertThat(handshake()).isFalse();
        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(tickets.find("ticket")).isPresent();
        verifyNoInteractions(decoder);
    }

    @Test
    void queryTokenUsesJwtAndTicketModel() {
        request.setParameter("access_token", "query");
        request.setParameter("model", BailianRealtimeModels.FLASH);
        when(decoder.decode("query"))
                .thenReturn(
                        Jwt.withTokenValue("query").header("alg", "none").subject("user").build());
        assertThat(handshake()).isTrue();
        assertThat(attributes)
                .containsEntry("userId", "user")
                .containsEntry("model", BailianRealtimeModels.PLUS);
        assertThat(tickets.find("ticket")).isEmpty();
    }

    @Test
    void demoTokenBypassesDecoderOnlyWhenEnabled() {
        security.setDemoTokenEnabled(true);
        security.setDemoToken("demo");
        security.setDemoUserId("user");
        request.setParameter("access_token", "demo");
        assertThat(handshake()).isTrue();
        verifyNoInteractions(decoder);
    }

    @Test
    void disabledDemoTokenMustPassJwtValidation() {
        security.setDemoToken("demo");
        request.setParameter("access_token", "demo");
        when(decoder.decode("demo")).thenThrow(new JwtException("invalid"));
        assertThat(handshake()).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void actionParserAcceptsOnlyTrueOrOne() {
        for (String value : new String[] {"true", " TRUE ", "1", " 1 "}) {
            assertThat(BailianRealtimeEventsHandshakeInterceptor.parseEnableAction(value)).isTrue();
        }
        for (String value : new String[] {null, "", "false", "yes", "0", "2"}) {
            assertThat(BailianRealtimeEventsHandshakeInterceptor.parseEnableAction(value))
                    .isFalse();
        }
    }
}
