package con.aidenjia.digitalhuman.conversation.bailian.webrtc;

/** SDP 交换后的一次性票据：只绑定用户与已选定模型。 */
public record BailianWebRtcTicket(String userId, String model) {}
