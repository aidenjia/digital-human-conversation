package con.aidenjia.digitalhuman.conversation.bailian.support;

import con.aidenjia.digitalhuman.conversation.bailian.prompt.RealtimePersonaPrompts;
import con.aidenjia.digitalhuman.conversation.bailian.prompt.RealtimeRulesPrompts;

/** 百炼 WebRTC signaling URL 与默认 instructions。 */
public final class BailianRealtimeUrls {

    private BailianRealtimeUrls() {}

    public static String buildWebRtcSignalUrl(String workspaceId, String model) {
        return "https://"
                + workspaceId.trim()
                + ".cn-beijing.maas.aliyuncs.com/api/v1/webrtc/realtime?model="
                + model;
    }

    /** 握手人设（{@code ## Role}）+ 口播规则。人设不得为空；规则仍只来自 classpath。 */
    public static String fixedInstructions(String persona) {
        if (persona == null || persona.isBlank()) {
            throw new IllegalArgumentException("persona is blank");
        }
        return RealtimePersonaPrompts.formatRoleBlock(persona)
                + "\n\n"
                + RealtimeRulesPrompts.rulesBody();
    }
}
