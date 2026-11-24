package con.aidenjia.digitalhuman.conversation.bailian.webrtc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import con.aidenjia.digitalhuman.conversation.api.bailian.webrtc.dto.ExchangeBailianWebRtcResponse;
import con.aidenjia.digitalhuman.conversation.bailian.support.BailianRealtimeModels;
import con.aidenjia.digitalhuman.conversation.exception.ApiException;
import con.aidenjia.digitalhuman.conversation.infrastructure.bailian.BailianRealtimeWebRtcSignalingClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 信令只换 SDP 与模型，人设不参与，非法模型不得打上游。 */
@ExtendWith(MockitoExtension.class)
class BailianWebRtcSignalingServiceTest {

    @Mock private BailianRealtimeWebRtcSignalingClient signalingClient;

    private InMemoryBailianWebRtcTicketStore tickets;
    private BailianWebRtcSignalingService service;

    @BeforeEach
    void setUp() {
        tickets = new InMemoryBailianWebRtcTicketStore();
        service = new BailianWebRtcSignalingService(signalingClient, tickets);
    }

    @Test
    @DisplayName("未传 model 用 flash，并写入只含用户与模型的票据")
    void blankModelUsesFlash() {
        when(signalingClient.exchange(BailianRealtimeModels.FLASH, "v=0")).thenReturn("v=answer");

        ExchangeBailianWebRtcResponse response = service.exchange("user-1", "v=0", "  ");

        assertThat(response.model()).isEqualTo(BailianRealtimeModels.FLASH);
        assertThat(response.answerSdp()).isEqualTo("v=answer");
        assertThat(tickets.find(response.relayId()))
                .contains(new BailianWebRtcTicket("user-1", BailianRealtimeModels.FLASH));
    }

    @Test
    @DisplayName("显式 plus 传给上游")
    void plusIsForwarded() {
        when(signalingClient.exchange(BailianRealtimeModels.PLUS, "v=0")).thenReturn("v=plus");

        ExchangeBailianWebRtcResponse response =
                service.exchange("user-1", "v=0", BailianRealtimeModels.PLUS);

        assertThat(response.model()).isEqualTo(BailianRealtimeModels.PLUS);
        verify(signalingClient).exchange(BailianRealtimeModels.PLUS, "v=0");
    }

    @Test
    @DisplayName("非法 model 拒绝且不调用上游")
    void illegalModelDoesNotCallUpstream() {
        assertThatThrownBy(() -> service.exchange("user-1", "v=0", "qwen-omni-turbo-realtime"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo(400);
        verify(signalingClient, never())
                .exchange(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
