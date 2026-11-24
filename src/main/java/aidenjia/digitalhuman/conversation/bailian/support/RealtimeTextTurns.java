package con.aidenjia.digitalhuman.conversation.bailian.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

/** Serializes text turns with generation cancellation; transport independent. */
public final class RealtimeTextTurns {
    private final ObjectMapper mapper;
    private final Consumer<String> upstream;
    private final Consumer<ObjectNode> downstream;
    private final Consumer<String> beginTurn;
    private final Set<String> seen = new LinkedHashSet<>();
    private final Set<String> obsolete = new LinkedHashSet<>();
    private boolean busy;
    private String activeId = "";
    private JsonNode pending;

    public RealtimeTextTurns(
            ObjectMapper mapper,
            Consumer<String> upstream,
            Consumer<ObjectNode> downstream,
            Consumer<String> beginTurn) {
        this.mapper = mapper;
        this.upstream = upstream;
        this.downstream = downstream;
        this.beginTurn = beginTurn;
    }

    public synchronized void submit(JsonNode event, boolean ready) {
        String id = event.path("id").asText("");
        String text = event.path("text").asText("").strip();
        if (!ready
                || !event.path("text").isTextual()
                || text.isBlank()
                || text.length() > 4000
                || id.isBlank()
                || id.length() > 80) {
            downstream.accept(
                    mapper.createObjectNode()
                            .put("type", "text.rejected")
                            .put("id", id)
                            .put("message", !ready ? "会话尚未就绪" : "输入无效或上一条提交尚在处理中（最多 4000 字）"));
            return;
        }
        if (seen.contains(id)) {
            downstream.accept(mapper.createObjectNode().put("type", "text.accepted").put("id", id));
            return;
        }
        if (pending != null) {
            downstream.accept(
                    mapper.createObjectNode()
                            .put("type", "text.rejected")
                            .put("id", id)
                            .put("message", "上一条提交仍在处理中"));
            return;
        }
        seen.add(id);
        trim(seen);
        pending = event;
        beginTurn.accept(text);
        if (!activeId.isBlank()) obsolete.add(activeId);
        downstream.accept(mapper.createObjectNode().put("type", "text.accepted").put("id", id));
        if (busy) {
            if (!activeId.isBlank()) upstream.accept("{\"type\":\"response.cancel\"}");
        } else drain();
    }

    /** Call for every outgoing response.create, including greeting/tool confirmation. */
    public synchronized void requested() {
        busy = true;
        activeId = "";
    }

    /** False means this event belongs to a superseded generation. */
    public synchronized boolean accept(JsonNode event) {
        String type = event.path("type").asText();
        String id =
                type.equals("response.created") || type.equals("response.done")
                        ? event.path("response").path("id").asText("")
                        : event.path("response_id").asText("");
        if (!id.isEmpty() && obsolete.contains(id) && !id.equals(activeId)) return false;
        if (type.equals("response.created")) {
            activeId = id;
            busy = true;
            if (pending != null) {
                obsolete.add(id);
                upstream.accept("{\"type\":\"response.cancel\"}");
            }
        }
        boolean stale =
                (!id.isEmpty() && obsolete.contains(id))
                        || (pending != null && type.startsWith("response."));
        if (type.equals("response.done") && (activeId.isEmpty() || id.equals(activeId))) {
            busy = false;
            activeId = "";
            drain();
        }
        return !stale;
    }

    private void drain() {
        if (pending == null) return;
        JsonNode request = pending;
        pending = null;
        downstream.accept(
                mapper.createObjectNode()
                        .put("type", "text.started")
                        .put("id", request.path("id").asText()));
        ObjectNode item = mapper.createObjectNode();
        item.put("type", "message").put("role", "user");
        item.putArray("content")
                .addObject()
                .put("type", "input_text")
                .put("text", request.path("text").asText().strip());
        ObjectNode create = mapper.createObjectNode().put("type", "conversation.item.create");
        create.set("item", item);
        upstream.accept(create.toString());
        ObjectNode response = mapper.createObjectNode().put("type", "response.create");
        response.putObject("response").putArray("modalities").add("audio").add("text");
        requested();
        upstream.accept(response.toString());
        trim(obsolete);
    }

    private static void trim(Set<String> ids) {
        if (ids.size() > 128) ids.remove(ids.iterator().next());
    }
}
