package con.aidenjia.digitalhuman.conversation.trtc.support;

import org.slf4j.MDC;

/** ConvoAI 请求级 SLF4J MDC：deviceId / channel / userId / agentId 等关联字段的唯一写入点。 */
public final class ConvoaiLogContext {

    public static final String MDC_DEVICE_ID = "deviceId";
    public static final String MDC_DIGITAL_HUMAN_TYPE = "digitalHumanType";
    public static final String MDC_USER_ID = "userId";
    public static final String MDC_CHANNEL = "channel";
    public static final String MDC_AGENT_ID = "agentId";

    private ConvoaiLogContext() {}

    /** join 入口：来自 HTTP body 的字段（Filter 或 Service 调用）。 */
    public static void bindJoinPreview(String deviceId, String digitalHumanType) {
        putIfText(MDC_DEVICE_ID, deviceId);
        putIfText(MDC_DIGITAL_HUMAN_TYPE, digitalHumanType);
    }

    /** leave 入口：来自 HTTP body 的 agentId。 */
    public static void bindLeave(String agentId) {
        putIfText(MDC_AGENT_ID, agentId);
    }

    /** join 编排：Service 生成 channel / userId 后 enrich，可选写入 agentId。 */
    public static void enrichAfterJoin(String channel, String userId, String agentId) {
        putIfText(MDC_CHANNEL, channel);
        putIfText(MDC_USER_ID, userId);
        putIfText(MDC_AGENT_ID, agentId);
    }

    public static void clear() {
        MDC.clear();
    }

    static void putIfText(String key, String value) {
        if (value != null && !value.isBlank()) {
            MDC.put(key, value);
        }
    }
}
