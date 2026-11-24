package con.aidenjia.digitalhuman.conversation.infrastructure.bailian;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import con.aidenjia.digitalhuman.conversation.bailian.action.RealtimeActionCatalog;
import con.aidenjia.digitalhuman.conversation.bailian.support.BailianRealtimeUrls;
import con.aidenjia.digitalhuman.conversation.config.BailianRealtimeProperties;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Builds JSON commands delivered through the WebRTC data channel. */
@Component
public class BailianRealtimeEventCommandFactory {

    private final BailianRealtimeProperties properties;
    private final ObjectMapper objectMapper;

    public BailianRealtimeEventCommandFactory(
            BailianRealtimeProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public String sessionUpdateJson(String persona, String voice) {
        ObjectNode session = objectMapper.createObjectNode();
        session.putArray("modalities").add("audio").add("text");
        session.put("instructions", BailianRealtimeUrls.fixedInstructions(persona));
        session.put("voice", voice);
        session.put("input_audio_format", "pcm");
        session.put("output_audio_format", "pcm");
        session.put("max_history_turns", clampHistory(properties.getMaxHistoryTurns()));
        session.put("enable_search", properties.isEnableSearch());
        ObjectNode turn = session.putObject("turn_detection");
        String type =
                StringUtils.hasText(properties.getTurnDetectionType())
                        ? properties.getTurnDetectionType().trim()
                        : "smart_turn";
        turn.put("type", type);

        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "session.update");
        root.set("session", session);
        return root.toString();
    }

    /**
     * 欢迎语 {@code response.done} 后补注册动作工具。只更新 tools / instructions，勿带 turn_detection（上游在已处理音频后拒绝修改）。
     */
    public String actionToolsUpdateJson(String persona) {
        ObjectNode session = objectMapper.createObjectNode();
        session.put(
                "instructions",
                BailianRealtimeUrls.fixedInstructions(persona)
                        + "\n\n"
                        + RealtimeActionCatalog.capabilityClause());
        session.set("tools", actionTools());

        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "session.update");
        root.set("session", session);
        return root.toString();
    }

    public String cancelResponseJson() {
        return objectMapper.createObjectNode().put("type", "response.cancel").toString();
    }

    public String deleteItemJson(String itemId) {
        if (!StringUtils.hasText(itemId)) {
            return null;
        }
        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "conversation.item.delete");
        root.put("item_id", itemId);
        return root.toString();
    }

    /** 会话就绪后口播固定欢迎语。须先写入一条用户消息，否则上游拒绝 {@code response.create}（无 user message）。 */
    public List<String> greetingJson(String greetingText) {
        if (!StringUtils.hasText(greetingText)) {
            return java.util.List.of();
        }
        String text = greetingText.trim();

        ObjectNode content = objectMapper.createObjectNode();
        content.put("type", "input_text");
        content.put("text", "（系统）请朗读固定欢迎语");
        ObjectNode item = objectMapper.createObjectNode();
        item.put("type", "message");
        item.put("role", "user");
        item.putArray("content").add(content);
        ObjectNode create = objectMapper.createObjectNode();
        create.put("type", "conversation.item.create");
        create.set("item", item);

        ObjectNode response = objectMapper.createObjectNode();
        response.putArray("modalities").add("audio").add("text");
        response.put("instructions", "禁止调用任何工具。只朗读这一句，不要补充、不要改写：" + text);
        ObjectNode createResponse = objectMapper.createObjectNode();
        createResponse.put("type", "response.create");
        createResponse.set("response", response);
        return List.of(create.toString(), createResponse.toString());
    }

    /** 工具已执行后触发二轮口播，只说确认句。 */
    public List<String> actionResultJson(String callId, String confirmation) {
        ObjectNode item = objectMapper.createObjectNode();
        item.put("type", "function_call_output");
        item.put("call_id", callId == null ? "" : callId);
        item.put("output", "已执行。请只朗读这一句，不要补充：" + confirmation);
        ObjectNode create = objectMapper.createObjectNode();
        create.put("type", "conversation.item.create");
        create.set("item", item);

        ObjectNode response = objectMapper.createObjectNode();
        response.putArray("modalities").add("audio").add("text");
        response.put("instructions", "只说这一句：" + confirmation);
        ObjectNode createResponse = objectMapper.createObjectNode();
        createResponse.put("type", "response.create");
        createResponse.set("response", response);
        return List.of(create.toString(), createResponse.toString());
    }

    private ArrayNode actionTools() {
        var tools = objectMapper.createArrayNode();
        ObjectNode fn = objectMapper.createObjectNode();
        fn.put("name", RealtimeActionCatalog.TOOL_NAME);
        fn.put(
                "description",
                "仅在用户明确要求数字人做场内动作或展示表情时调用本工具，不要只用口播确认代替。"
                        + "用户要求看开心、难过、生气、惊讶的样子或表情，分别使用 expression_happy、expression_sad、expression_angry、expression_surprised。"
                        + "仅当用户明确要求放大、拉近镜头或缩小、拉远镜头时才使用 zoom_in 或 zoom_out；询问表情是什么样不等于要求调整镜头。"
                        + "朗读、背诗、念古诗、讲故事和唱歌等语音内容请求不调用本工具，由语音模型直接回答。"
                        + "command 只能是枚举值之一。");
        ObjectNode params = fn.putObject("parameters");
        params.put("type", "object");
        ObjectNode props = params.putObject("properties");
        ObjectNode command = props.putObject("command");
        command.put("type", "string");
        command.put("description", "动作命令");
        var enums = command.putArray("enum");
        for (String value : RealtimeActionCatalog.commands()) {
            enums.add(value);
        }
        params.putArray("required").add("command");
        ObjectNode tool = objectMapper.createObjectNode();
        tool.put("type", "function");
        tool.set("function", fn);
        tools.add(tool);
        return tools;
    }

    private static int clampHistory(int configured) {
        if (configured < 1) {
            return 1;
        }
        if (configured > 50) {
            return 50;
        }
        return configured;
    }
}
