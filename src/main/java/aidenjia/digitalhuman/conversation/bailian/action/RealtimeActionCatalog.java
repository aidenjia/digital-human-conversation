package con.aidenjia.digitalhuman.conversation.bailian.action;

import java.util.Arrays;
import java.util.Optional;

/** Realtime 身体动作目录；朗读、唱歌等语音内容由实时模型直接处理。 */
public enum RealtimeActionCatalog {
    WAVE("wave", "挥手", "你好呀，很高兴见到你"),
    NOD("nod", "点头", "嗯，我在认真听"),
    DANCE("dance", "跳舞", "好的，让我为你跳个舞"),
    PHOTO("photo", "拍照", "好的，让我为你拍张照"),
    GIFT("gift", "礼物", "好的，让我送你个礼物"),
    HEARTBEAT("heartbeat", "靠近/表白/喜欢", "好的，让我靠近你"),
    EXPRESSION_HAPPY("expression_happy", "开心表情", "像这样，我很开心"),
    EXPRESSION_SAD("expression_sad", "难过表情", "像这样，有点难过"),
    EXPRESSION_ANGRY("expression_angry", "生气表情", "哼，我生气啦"),
    EXPRESSION_SURPRISED("expression_surprised", "惊讶表情", "哇，吓我一跳"),
    CHANGE_SCENE("change_scene", "换场景", "好的，让我换个场景"),
    ZOOM_IN("zoom_in", "放大（拉近镜头）", "好的，让我放大一点"),
    ZOOM_OUT("zoom_out", "缩小（拉远镜头）", "好的，让我缩小一点"),
    RESET_DEFAULT("reset_default", "恢复默认", "好的，让我恢复默认"),
    GO_BACK_MAIN("go_back_main", "返回首页", "好的，让我返回首页");

    public static final String TOOL_NAME = "digital_human_action";

    private final String command;
    private final String displayName;
    private final String confirmation;

    RealtimeActionCatalog(String command, String displayName, String confirmation) {
        this.command = command;
        this.displayName = displayName;
        this.confirmation = confirmation;
    }

    public String command() {
        return command;
    }

    public String displayName() {
        return displayName;
    }

    public String confirmation() {
        return confirmation;
    }

    public static Optional<RealtimeActionCatalog> fromCommand(String command) {
        if (command == null || command.isBlank()) {
            return Optional.empty();
        }
        String trimmed = command.trim();
        return Arrays.stream(values()).filter(a -> a.command.equals(trimmed)).findFirst();
    }

    public static String[] commands() {
        return Arrays.stream(values()).map(RealtimeActionCatalog::command).toArray(String[]::new);
    }

    /** 追加到人设：用户问「有哪些功能」时可口语列出。 */
    public static String capabilityClause() {
        String names =
                Arrays.stream(values())
                        .map(RealtimeActionCatalog::displayName)
                        .reduce((a, b) -> a + "、" + b)
                        .orElse("");
        return "你可以当场做这些动作："
                + names
                + "。用户询问你有哪些功能或能做什么时，用口语列出这些动作。"
                + "用户要求当场做一个动作时必须调用 digital_human_action，不要在调用工具之前把确认句说完。";
    }
}
