package con.aidenjia.digitalhuman.conversation.infrastructure.bailian;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Locale;
import okhttp3.Response;

/** Bounded error metadata only; never retain or log raw provider messages or SDP. */
record BailianSignalingFailure(String code, String reason, String requestId) {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int MAX_ERROR_BYTES = 16_384;

    static BailianSignalingFailure read(Response response, String apiKey) {
        String requestId = "";
        for (String header :
                new String[] {"x-request-id", "x-dashscope-request-id", "request-id"}) {
            requestId = safeIdentifier(response.header(header), apiKey);
            if (!requestId.isEmpty()) break;
        }
        String code = "", reason = "unknown";
        try {
            if (response.body() != null) {
                byte[] bytes = response.body().byteStream().readNBytes(MAX_ERROR_BYTES + 1);
                if (bytes.length <= MAX_ERROR_BYTES) {
                    JsonNode root = JSON.readTree(bytes);
                    if (root != null && root.isObject()) {
                        JsonNode error = root.path("error").isObject() ? root.path("error") : root;
                        code =
                                safeIdentifier(
                                        error.path("code").asText(error.path("Code").asText()),
                                        apiKey);
                        reason =
                                reason(
                                        code,
                                        error.path("message")
                                                .asText(error.path("Message").asText()));
                        String bodyId =
                                safeIdentifier(
                                        root.path("request_id")
                                                .asText(
                                                        root.path("requestId")
                                                                .asText(
                                                                        root.path("RequestId")
                                                                                .asText())),
                                        apiKey);
                        if (!bodyId.isEmpty()) requestId = bodyId;
                    }
                }
            }
        } catch (IOException ignored) {
            // A malformed/unreadable error must not hide the original HTTP status.
        }
        return new BailianSignalingFailure(
                code.isEmpty() ? "unknown" : code,
                reason,
                requestId.isEmpty() ? "unavailable" : requestId);
    }

    static String safeIdentifier(String value, String apiKey) {
        if (value == null || !value.matches("[A-Za-z0-9][A-Za-z0-9_.:-]{0,127}")) return "";
        if (value.toLowerCase(Locale.ROOT).contains("sk-")
                || (!apiKey.isBlank() && value.contains(apiKey.trim()))) return "";
        return value;
    }

    private static String reason(String code, String message) {
        String detail = (code + " " + message).toLowerCase(Locale.ROOT);
        if (detail.contains("arrearage") || detail.contains("good standing"))
            return "account_status";
        if (detail.contains("workspace")) return "workspace_access";
        if (detail.contains("unpurchased") || detail.contains("not activated"))
            return "service_entitlement";
        if (detail.contains("model")
                && (detail.contains("denied") || detail.contains("not authorized")))
            return "model_access";
        if (detail.contains("invalidapikey") || detail.contains("invalid_api_key"))
            return "authentication";
        if (detail.contains("access denied")
                || detail.contains("accessdenied")
                || detail.contains("access_denied")) return "access_denied";
        return "unknown";
    }
}
