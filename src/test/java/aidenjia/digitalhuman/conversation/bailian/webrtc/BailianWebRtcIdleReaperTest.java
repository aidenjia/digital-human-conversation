package con.aidenjia.digitalhuman.conversation.bailian.webrtc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import con.aidenjia.digitalhuman.conversation.bailian.support.BailianRealtimeModels;
import con.aidenjia.digitalhuman.conversation.config.BailianRealtimeProperties;
import con.aidenjia.digitalhuman.conversation.infrastructure.bailian.BailianRealtimeEventCommandFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

/** Idle 意图：超时无客户端活动则关连，活跃会话不受影响。 */
class BailianWebRtcIdleReaperTest {

    @Test
    @DisplayName("空闲超时关闭；刚 touch 的会话保留")
    void reapsOnlyIdleSessions() throws Exception {
        BailianRealtimeEventsRegistry registry = new BailianRealtimeEventsRegistry();
        BailianRealtimeProperties properties = new BailianRealtimeProperties();
        properties.setIdleTimeoutSeconds(5);

        WebSocketSession idleClient = mock(WebSocketSession.class);
        when(idleClient.getId()).thenReturn("idle");
        when(idleClient.isOpen()).thenReturn(true);
        BailianRealtimeEventsSession idle =
                new BailianRealtimeEventsSession(
                        idleClient,
                        mock(BailianRealtimeEventCommandFactory.class),
                        properties,
                        new ObjectMapper(),
                        registry,
                        false,
                        BailianRealtimeModels.FLASH,
                        "你叫黛黛",
                        "longanqian");
        // 人为把活动时间拨回超时之外
        java.lang.reflect.Field field =
                BailianRealtimeEventsSession.class.getDeclaredField("lastClientActivityMs");
        field.setAccessible(true);
        ((java.util.concurrent.atomic.AtomicLong) field.get(idle))
                .set(System.currentTimeMillis() - 10_000L);
        registry.register(idle);

        WebSocketSession activeClient = mock(WebSocketSession.class);
        when(activeClient.getId()).thenReturn("active");
        when(activeClient.isOpen()).thenReturn(true);
        BailianRealtimeEventsSession active =
                new BailianRealtimeEventsSession(
                        activeClient,
                        mock(BailianRealtimeEventCommandFactory.class),
                        properties,
                        new ObjectMapper(),
                        registry,
                        false,
                        BailianRealtimeModels.FLASH,
                        "你叫黛黛",
                        "longanqian");
        active.handleClientText("{\"type\":\"ping\"}");
        registry.register(active);

        BailianWebRtcIdleReaper reaper = new BailianWebRtcIdleReaper(registry, properties);
        reaper.reapIdle();

        verify(idleClient).close(any());
        verify(activeClient, never()).close(any());
    }
}
