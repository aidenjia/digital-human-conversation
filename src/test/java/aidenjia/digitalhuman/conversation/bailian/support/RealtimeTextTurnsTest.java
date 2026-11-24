package con.aidenjia.digitalhuman.conversation.bailian.support;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RealtimeTextTurnsTest {
    final ObjectMapper mapper = new ObjectMapper();
    final List<String> commands = new ArrayList<>();
    final List<ObjectNode> replies = new ArrayList<>();
    final List<String> users = new ArrayList<>();
    final RealtimeTextTurns turns =
            new RealtimeTextTurns(mapper, commands::add, replies::add, users::add);

    ObjectNode request(String id, String text) {
        return mapper.createObjectNode().put("id", id).put("text", text);
    }

    boolean event(String json) throws Exception {
        return turns.accept(mapper.readTree(json));
    }

    @Test
    void lateOldCompletionCannotReleaseANewerQueuedTurn() throws Exception {
        event("{\"type\":\"response.created\",\"response\":{\"id\":\"old\"}}");
        turns.submit(request("next", "第二问"), true);
        event("{\"type\":\"response.done\",\"response\":{\"id\":\"old\"}}");
        turns.submit(request("third", "第三问"), true);
        assertEquals(3, commands.size());
        event("{\"type\":\"response.done\",\"response\":{\"id\":\"old\"}}");
        assertEquals(3, commands.size());
    }

    @Test
    void duplicateReceiptWhileWaitingForCancellationDoesNotCreateAnotherTurn() throws Exception {
        event("{\"type\":\"response.created\",\"response\":{\"id\":\"old\"}}");
        turns.submit(request("next", "下一问"), true);
        turns.submit(request("next", "下一问"), true);
        assertEquals("text.accepted", replies.get(1).path("type").asText());
        assertEquals(1, users.size());
    }

    @Test
    void createsUserTextAndAudioResponseOnce() throws Exception {
        turns.submit(request("t1", "  你好\n世界  "), true);
        turns.submit(request("t1", "ignored"), true);
        assertEquals(List.of("你好\n世界"), users);
        assertEquals(2, commands.size());
        assertEquals(
                "input_text", mapper.readTree(commands.get(0)).at("/item/content/0/type").asText());
        assertEquals(
                "你好\n世界", mapper.readTree(commands.get(0)).at("/item/content/0/text").asText());
        assertEquals(
                "audio", mapper.readTree(commands.get(1)).at("/response/modalities/0").asText());
    }

    @Test
    void rejectsInvalidInputWithoutCancelingCurrentResponse() {
        turns.submit(request("a", " "), true);
        turns.submit(request("b", "x".repeat(4001)), true);
        turns.submit(request("c", "你好"), false);
        assertTrue(commands.isEmpty());
        assertTrue(users.isEmpty());
        assertTrue(replies.stream().allMatch(r -> r.path("type").asText().equals("text.rejected")));
    }

    @Test
    void cancelsBeforeCreatingNextAndDropsLateAudioAndToolCalls() throws Exception {
        event("{\"type\":\"response.created\",\"response\":{\"id\":\"old\"}}");
        turns.submit(request("next", "下一问"), true);
        assertEquals(1, commands.size());
        assertEquals("response.cancel", mapper.readTree(commands.get(0)).path("type").asText());
        assertFalse(event("{\"type\":\"response.audio.delta\",\"response_id\":\"old\"}"));
        assertFalse(event("{\"type\":\"response.done\",\"response\":{\"id\":\"old\"}}"));
        assertEquals(3, commands.size());
        event("{\"type\":\"response.created\",\"response\":{\"id\":\"new\"}}");
        assertFalse(
                event(
                        "{\"type\":\"response.function_call_arguments.done\",\"response_id\":\"old\"}"));
        assertTrue(event("{\"type\":\"response.audio_transcript.delta\",\"response_id\":\"new\"}"));
    }

    @Test
    void cancelsGreetingThatHasBeenRequestedButNotYetCreated() throws Exception {
        turns.requested();
        turns.submit(request("t", "你好"), true);
        assertTrue(commands.isEmpty());
        assertFalse(event("{\"type\":\"response.created\",\"response\":{\"id\":\"greeting\"}}"));
        assertEquals(1, commands.size());
        event("{\"type\":\"response.done\",\"response\":{\"id\":\"greeting\"}}");
        assertEquals(3, commands.size());
    }
}
