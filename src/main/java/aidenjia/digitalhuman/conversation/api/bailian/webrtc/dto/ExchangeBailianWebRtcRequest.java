package con.aidenjia.digitalhuman.conversation.api.bailian.webrtc.dto;

import jakarta.validation.constraints.NotBlank;

public record ExchangeBailianWebRtcRequest(
        @NotBlank(message = "offerSdp is required") String offerSdp, String model) {}
