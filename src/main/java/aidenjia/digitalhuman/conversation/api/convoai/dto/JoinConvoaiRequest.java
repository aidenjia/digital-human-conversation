package con.aidenjia.digitalhuman.conversation.api.convoai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record JoinConvoaiRequest(
        @NotBlank(message = "deviceId is required") String deviceId,
        @NotNull(message = "digitalHumanType is required") DigitalHumanType digitalHumanType,
        String timezone) {}
