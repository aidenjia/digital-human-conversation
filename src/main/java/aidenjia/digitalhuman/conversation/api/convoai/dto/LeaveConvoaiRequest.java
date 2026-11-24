package con.aidenjia.digitalhuman.conversation.api.convoai.dto;

import jakarta.validation.constraints.NotBlank;

public record LeaveConvoaiRequest(@NotBlank(message = "agentId is required") String agentId) {}
