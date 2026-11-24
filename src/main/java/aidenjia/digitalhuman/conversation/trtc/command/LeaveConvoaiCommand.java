package con.aidenjia.digitalhuman.conversation.trtc.command;

/** Service 入参：按 join 响应中的 {@code agentId} 停止智能体。 */
public record LeaveConvoaiCommand(String agentId) {}
