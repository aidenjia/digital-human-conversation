package con.aidenjia.digitalhuman.conversation.api.bailian.webrtc;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class BailianRealtimeEventsWebSocketConfig implements WebSocketConfigurer {
    private final BailianRealtimeEventsWebSocketHandler eventsHandler;
    private final BailianRealtimeEventsHandshakeInterceptor eventsHandshakeInterceptor;

    public BailianRealtimeEventsWebSocketConfig(
            BailianRealtimeEventsWebSocketHandler eventsHandler,
            BailianRealtimeEventsHandshakeInterceptor eventsHandshakeInterceptor) {
        this.eventsHandler = eventsHandler;
        this.eventsHandshakeInterceptor = eventsHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(eventsHandler, "/ws/bailian/realtime-events")
                .addInterceptors(eventsHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
