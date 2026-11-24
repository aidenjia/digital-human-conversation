package con.aidenjia.digitalhuman.conversation.bailian.support;

import java.util.Set;

/** 客户端握手可选的百炼 Realtime 模型。未传时用 flash，其它值一律拒绝。 */
public final class BailianRealtimeModels {

    public static final String FLASH = "qwen-audio-3.0-realtime-flash";

    public static final String PLUS = "qwen-audio-3.0-realtime-plus";

    private static final Set<String> ALLOWED = Set.of(FLASH, PLUS);

    private BailianRealtimeModels() {}

    /** 空白视为未传，回落 {@link #FLASH}。 */
    public static String resolve(String model) {
        if (model == null || model.isBlank()) {
            return FLASH;
        }
        return model;
    }

    public static boolean isAllowed(String model) {
        return model != null && ALLOWED.contains(model);
    }
}
