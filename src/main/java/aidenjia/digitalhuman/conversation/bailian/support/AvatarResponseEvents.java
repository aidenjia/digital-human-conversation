package con.aidenjia.digitalhuman.conversation.bailian.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** Additive lifecycle events for cancelling response-scoped avatar cues. */
public final class AvatarResponseEvents {
    private AvatarResponseEvents() {}

    public static ObjectNode lifecycle(JsonNode event, ObjectMapper mapper) {
        String id = event.path("response").path("id").asText("");
        if (id.isBlank()) {
            return null;
        }
        String type = event.path("type").asText("");
        String status = event.path("response").path("status").asText("");
        String phase =
                "response.created".equals(type)
                        ? "created"
                        : ("cancelled".equals(status) || "failed".equals(status))
                                ? "cancelled"
                                : "done";
        return mapper.createObjectNode()
                .put("type", "avatar.response")
                .put("responseId", id)
                .put("phase", phase);
    }
}
