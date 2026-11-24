package con.aidenjia.digitalhuman.conversation.api.bailian.webrtc;

import com.fasterxml.jackson.databind.ObjectMapper;
import con.aidenjia.digitalhuman.conversation.bailian.support.BailianRealtimeModels;
import con.aidenjia.digitalhuman.conversation.bailian.webrtc.BailianRealtimeEventsRegistry;
import con.aidenjia.digitalhuman.conversation.bailian.webrtc.BailianRealtimeEventsSession;
import con.aidenjia.digitalhuman.conversation.config.BailianRealtimeProperties;
import con.aidenjia.digitalhuman.conversation.infrastructure.avatar.AvatarDecisionClient;
import con.aidenjia.digitalhuman.conversation.infrastructure.bailian.BailianRealtimeEventCommandFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/** WebRTC 控制通道。不向上游百炼再开 WebSocket。 */
@Component
public class BailianRealtimeEventsWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log =
            LoggerFactory.getLogger(BailianRealtimeEventsWebSocketHandler.class);

    private final BailianRealtimeEventCommandFactory upstreamClient;
    private final BailianRealtimeProperties properties;
    private final ObjectMapper objectMapper;
    private final BailianRealtimeEventsRegistry registry;
    private final AvatarDecisionClient decisionClient;

    public BailianRealtimeEventsWebSocketHandler(
            BailianRealtimeEventCommandFactory upstreamClient,
            BailianRealtimeProperties properties,
            ObjectMapper objectMapper,
            BailianRealtimeEventsRegistry registry,
            AvatarDecisionClient decisionClient) {
        this.upstreamClient = upstreamClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.registry = registry;
        this.decisionClient = decisionClient;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String model =
                (String)
                        session.getAttributes()
                                .get(BailianRealtimeEventsHandshakeInterceptor.ATTR_MODEL);
        String persona =
                (String)
                        session.getAttributes()
                                .get(BailianRealtimeEventsHandshakeInterceptor.ATTR_PERSONA);
        String voice =
                (String)
                        session.getAttributes()
                                .get(BailianRealtimeEventsHandshakeInterceptor.ATTR_VOICE);
        if (!BailianRealtimeModels.isAllowed(model)
                || persona == null
                || persona.isBlank()
                || voice == null
                || voice.isBlank()) {
            throw new IllegalStateException("webrtc events params missing after handshake");
        }
        boolean enableAction =
                Boolean.TRUE.equals(
                        session.getAttributes()
                                .get(BailianRealtimeEventsHandshakeInterceptor.ATTR_ENABLE_ACTION));
        log.info("Bailian realtime events connected model={}", model);
        BailianRealtimeEventsSession events =
                new BailianRealtimeEventsSession(
                        session,
                        upstreamClient,
                        properties,
                        objectMapper,
                        registry,
                        enableAction,
                        model,
                        persona,
                        voice,
                        decisionClient);
        registry.register(events);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        BailianRealtimeEventsSession events = registry.get(session.getId());
        if (events == null) {
            return;
        }
        events.handleClientText(message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        BailianRealtimeEventsSession events = registry.get(session.getId());
        if (events != null) {
            events.closeBoth(status);
        }
        log.info("Bailian realtime events closed code={}", status.getCode());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("Bailian realtime events transport error");
        BailianRealtimeEventsSession events = registry.get(session.getId());
        if (events != null) {
            events.closeBoth(CloseStatus.SERVER_ERROR);
        }
    }
}
