package con.aidenjia.digitalhuman.conversation.api.convoai.dto;

/** 成功停止对话式智能体时 {@code ApiResponse.data} 载荷。 */
public record LeaveConvoaiResult(String agentId, String status) {}
