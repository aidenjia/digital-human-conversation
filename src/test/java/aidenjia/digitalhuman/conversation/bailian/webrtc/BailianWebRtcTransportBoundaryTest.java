package con.aidenjia.digitalhuman.conversation.bailian.webrtc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class BailianWebRtcTransportBoundaryTest {
    @Test
    void transportHasNoPcmRelayOrUpstreamWebSocket() throws Exception {
        for (String name :
                new String[] {
                    "bailian.relay.BailianRealtimeRelaySession",
                    "api.bailian.ws.BailianRealtimeWebSocketConfig",
                    "infrastructure.bailian.BailianRealtimeUpstreamClient"
                }) {
            assertThatThrownBy(
                            () -> Class.forName("con.aidenjia.digitalhuman.conversation." + name))
                    .isInstanceOf(ClassNotFoundException.class);
        }
        Class<?> factory =
                Class.forName(
                        "con.aidenjia.digitalhuman.conversation.infrastructure.bailian.BailianRealtimeEventCommandFactory");
        assertThat(factory.getDeclaredMethods())
                .noneMatch(
                        method ->
                                method.getName().equals("connect")
                                        || method.getName().equals("appendAudio"));
    }
}
