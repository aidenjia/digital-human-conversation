package con.aidenjia.digitalhuman.conversation.bailian.webrtc;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import con.aidenjia.digitalhuman.conversation.config.BailianRealtimeProperties;
import con.aidenjia.digitalhuman.conversation.infrastructure.bailian.BailianRealtimeEventCommandFactory;
import org.junit.jupiter.api.Test;

class BailianRealtimeEventCommandFactoryTest {
    final ObjectMapper mapper = new ObjectMapper();
    final BailianRealtimeProperties properties = new BailianRealtimeProperties();
    final BailianRealtimeEventCommandFactory factory =
            new BailianRealtimeEventCommandFactory(properties, mapper);

    @Test
    void sessionUpdateRetainsVoiceInstructionsVadAndHistoryBounds() throws Exception {
        properties.setMaxHistoryTurns(999);
        var session =
                mapper.readTree(factory.sessionUpdateJson("persona", "voice")).path("session");
        assertThat(session.path("voice").asText()).isEqualTo("voice");
        assertThat(session.path("instructions").asText()).contains("persona");
        assertThat(session.path("max_history_turns").asInt()).isEqualTo(50);
        assertThat(session.path("turn_detection").path("type").asText()).isNotBlank();
        properties.setMaxHistoryTurns(0);
        assertThat(
                        mapper.readTree(factory.sessionUpdateJson("persona", "voice"))
                                .path("session")
                                .path("max_history_turns")
                                .asInt())
                .isEqualTo(1);
    }

    @Test
    void greetingAndActionCommandsRemainDataChannelJson() throws Exception {
        assertThat(factory.greetingJson(" ")).isEmpty();
        var greeting = factory.greetingJson("hello");
        assertThat(greeting).hasSize(2);
        assertThat(mapper.readTree(greeting.get(0)).path("type").asText())
                .isEqualTo("conversation.item.create");
        assertThat(mapper.readTree(greeting.get(1)).path("response").path("instructions").asText())
                .contains("hello");
        var action = factory.actionResultJson("call", "done");
        assertThat(mapper.readTree(action.get(0)).path("item").path("call_id").asText())
                .isEqualTo("call");
        assertThat(mapper.readTree(factory.cancelResponseJson()).path("type").asText())
                .isEqualTo("response.cancel");
        assertThat(factory.deleteItemJson(" ")).isNull();
        assertThat(mapper.readTree(factory.deleteItemJson("item")).path("item_id").asText())
                .isEqualTo("item");
        var tools = mapper.readTree(factory.actionToolsUpdateJson("persona")).path("session");
        assertThat(tools.has("turn_detection")).isFalse();
        assertThat(tools.path("tools").size()).isEqualTo(1);
    }

    @Test
    void speechContentIsNotAdvertisedAsASingingBodyAction() throws Exception {
        var session = mapper.readTree(factory.actionToolsUpdateJson("persona")).path("session");
        var function = session.path("tools").path(0).path("function");
        var commands = function.path("parameters").path("properties").path("command").path("enum");
        assertThat(commands.toString()).doesNotContain("\"sing\"");
        assertThat(commands.toString()).contains("\"wave\"", "\"nod\"");
        assertThat(function.path("description").asText()).contains("朗读", "不调用");
        assertThat(session.path("instructions").asText()).contains("古诗", "朗读", "舞台说明");
    }

    @Test
    void explicitExpressionsAreDistinctFromCameraZoom() throws Exception {
        var session = mapper.readTree(factory.actionToolsUpdateJson("persona")).path("session");
        var function = session.path("tools").path(0).path("function");
        var commands = function.path("parameters").path("properties").path("command").path("enum");
        assertThat(commands.toString())
                .contains(
                        "expression_happy",
                        "expression_sad",
                        "expression_angry",
                        "expression_surprised");
        assertThat(function.path("description").asText()).contains("惊讶", "镜头");
    }
}
