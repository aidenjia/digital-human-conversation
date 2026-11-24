package con.aidenjia.digitalhuman.conversation.bailian.webrtc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import con.aidenjia.digitalhuman.conversation.bailian.support.BailianRealtimeModels;
import con.aidenjia.digitalhuman.conversation.config.BailianRealtimeProperties;
import con.aidenjia.digitalhuman.conversation.infrastructure.bailian.BailianRealtimeEventCommandFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/** Drain 意图：发版关停时广播 server.draining，再关闭会话，并拒绝新连（via isDraining）。 */
class BailianWebRtcDrainLifecycleTest {

    @Test
    @DisplayName("stop 同步触发：广播 draining 后关闭会话")
    void drainBroadcastsThenCloses() throws Exception {
        BailianRealtimeEventsRegistry registry = new BailianRealtimeEventsRegistry();
        BailianRealtimeProperties properties = new BailianRealtimeProperties();
        properties.setDrainGraceSeconds(0);

        WebSocketSession client = mock(WebSocketSession.class);
        when(client.getId()).thenReturn("s1");
        when(client.isOpen()).thenReturn(true);
        List<String> messages = new ArrayList<>();
        org.mockito.Mockito.doAnswer(
                        inv -> {
                            messages.add(((TextMessage) inv.getArgument(0)).getPayload());
                            return null;
                        })
                .when(client)
                .sendMessage(any(TextMessage.class));

        BailianRealtimeEventsSession session =
                new BailianRealtimeEventsSession(
                        client,
                        mock(BailianRealtimeEventCommandFactory.class),
                        properties,
                        new ObjectMapper(),
                        registry,
                        false,
                        BailianRealtimeModels.FLASH,
                        "你叫黛黛",
                        "longanqian");
        registry.register(session);

        Executor sync = Runnable::run;
        BailianWebRtcDrainLifecycle lifecycle =
                new BailianWebRtcDrainLifecycle(registry, properties, sync);

        lifecycle.start();
        assertThat(lifecycle.isDraining()).isFalse();
        assertThat(lifecycle.getPhase()).isEqualTo(Integer.MIN_VALUE + 1);

        lifecycle.stop(() -> {});

        assertThat(lifecycle.isDraining()).isTrue();
        assertThat(messages).anyMatch(m -> m.contains("server.draining"));
        verify(client).close(any());
        assertThat(lifecycle.isRunning()).isFalse();
        assertThat(registry.all()).isEmpty();
    }

    @Test
    void gracePeriodDefersCloseAndCallbackUntilAfterDraining() throws Exception {
        var registry = new BailianRealtimeEventsRegistry();
        var properties = new BailianRealtimeProperties();
        properties.setDrainGraceSeconds(1);
        var client = mock(WebSocketSession.class);
        when(client.getId()).thenReturn("grace");
        when(client.isOpen()).thenReturn(true);
        registry.register(
                new BailianRealtimeEventsSession(
                        client,
                        new BailianRealtimeEventCommandFactory(properties, new ObjectMapper()),
                        properties,
                        new ObjectMapper(),
                        registry,
                        false,
                        BailianRealtimeModels.FLASH,
                        "persona",
                        "voice"));
        var drainingSent = new java.util.concurrent.CountDownLatch(1);
        var releaseDraining = new java.util.concurrent.CountDownLatch(1);
        org.mockito.Mockito.doAnswer(
                        inv -> {
                            drainingSent.countDown();
                            releaseDraining.await();
                            return null;
                        })
                .when(client)
                .sendMessage(any(TextMessage.class));
        var callback = new java.util.concurrent.CountDownLatch(1);
        var lifecycle =
                new BailianWebRtcDrainLifecycle(
                        registry, properties, task -> Thread.ofVirtual().start(task));
        lifecycle.start();
        lifecycle.stop(callback::countDown);
        try {
            assertThat(drainingSent.await(2, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            verify(client, org.mockito.Mockito.never()).close(any());
            assertThat(callback.getCount()).isEqualTo(1);
        } finally {
            releaseDraining.countDown();
        }
        assertThat(callback.await(3, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        verify(client).close(org.springframework.web.socket.CloseStatus.GOING_AWAY);
    }
}
