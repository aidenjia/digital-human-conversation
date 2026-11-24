package con.aidenjia.digitalhuman.conversation.api.bailian.webrtc;

import con.aidenjia.digitalhuman.conversation.bailian.webrtc.BailianWebRtcDrainLifecycle;
import con.aidenjia.digitalhuman.conversation.bailian.webrtc.BailianWebRtcTicket;
import con.aidenjia.digitalhuman.conversation.bailian.webrtc.BailianWebRtcTicketStore;
import con.aidenjia.digitalhuman.conversation.config.ApiSecurityProperties;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/** WebRTC 控制连接握手。人设、音色、动作开关来自 query；模型来自 SDP 票据。query {@code model} 不覆盖票据。 */
@Component
public class BailianRealtimeEventsHandshakeInterceptor implements HandshakeInterceptor {

    public static final String ATTR_USER_ID = "userId";
    public static final String ATTR_ENABLE_ACTION = "enableAction";
    public static final String ATTR_MODEL = "model";
    public static final String ATTR_PERSONA = "persona";
    public static final String ATTR_VOICE = "voice";

    private static final String QUERY_ACCESS_TOKEN = "access_token";
    private static final String QUERY_RELAY_ID = "relayId";
    private static final String QUERY_ENABLE_ACTION = "enableAction";
    private static final String QUERY_PERSONA = "persona";
    private static final String QUERY_VOICE = "voice";

    private static final Logger log =
            LoggerFactory.getLogger(BailianRealtimeEventsHandshakeInterceptor.class);

    private final JwtDecoder jwtDecoder;
    private final BailianWebRtcDrainLifecycle drainLifecycle;
    private final ApiSecurityProperties apiSecurity;
    private final BailianWebRtcTicketStore ticketStore;

    public BailianRealtimeEventsHandshakeInterceptor(
            JwtDecoder jwtDecoder,
            BailianWebRtcDrainLifecycle drainLifecycle,
            ApiSecurityProperties apiSecurity,
            BailianWebRtcTicketStore ticketStore) {
        this.jwtDecoder = jwtDecoder;
        this.drainLifecycle = drainLifecycle;
        this.apiSecurity = apiSecurity;
        this.ticketStore = ticketStore;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        if (drainLifecycle.isDraining()) {
            response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
            return false;
        }
        if (!(request instanceof ServletServerHttpRequest servlet)) {
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return false;
        }
        String token = resolveToken(servlet);
        if (!StringUtils.hasText(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        String persona = queryParam(servlet, QUERY_PERSONA);
        String voice = queryParam(servlet, QUERY_VOICE);
        if (!StringUtils.hasText(persona) || !StringUtils.hasText(voice)) {
            log.warn("Bailian realtime events persona or voice rejected");
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return false;
        }
        String relayId = queryParam(servlet, QUERY_RELAY_ID);
        if (!StringUtils.hasText(relayId)) {
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return false;
        }
        boolean enableAction = parseEnableAction(queryParam(servlet, QUERY_ENABLE_ACTION));
        String trimmed = token.trim();
        String userId;
        if (matchesDemoToken(trimmed)) {
            userId = apiSecurity.getDemoUserId();
        } else {
            try {
                Jwt jwt = jwtDecoder.decode(trimmed);
                userId = jwt.getSubject();
                if (!StringUtils.hasText(userId)) {
                    response.setStatusCode(HttpStatus.UNAUTHORIZED);
                    return false;
                }
            } catch (JwtException ex) {
                log.warn("Bailian realtime events JWT rejected");
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }
        }
        Optional<BailianWebRtcTicket> ticket = ticketStore.consume(relayId, userId.trim());
        if (ticket.isEmpty()) {
            log.warn("Bailian realtime events ticket rejected");
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return false;
        }
        attributes.put(ATTR_USER_ID, userId.trim());
        attributes.put(ATTR_ENABLE_ACTION, enableAction);
        attributes.put(ATTR_MODEL, ticket.get().model());
        attributes.put(ATTR_PERSONA, persona.trim());
        attributes.put(ATTR_VOICE, voice.trim());
        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
        // no-op
    }

    private boolean matchesDemoToken(String token) {
        return apiSecurity.isDemoTokenEnabled()
                && StringUtils.hasText(apiSecurity.getDemoToken())
                && token.equals(apiSecurity.getDemoToken());
    }

    static boolean parseEnableAction(String raw) {
        return raw != null && ("true".equalsIgnoreCase(raw.trim()) || "1".equals(raw.trim()));
    }

    private static String resolveToken(ServletServerHttpRequest servlet) {
        String header = servlet.getServletRequest().getHeader("Authorization");
        if (StringUtils.hasText(header) && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return header.substring(7).trim();
        }
        return queryParam(servlet, QUERY_ACCESS_TOKEN);
    }

    private static String queryParam(ServletServerHttpRequest servlet, String name) {
        String[] values = servlet.getServletRequest().getParameterMap().get(name);
        if (values != null && values.length > 0 && StringUtils.hasText(values[0])) {
            return values[0].trim();
        }
        return null;
    }
}
