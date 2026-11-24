package con.aidenjia.digitalhuman.conversation.infrastructure.convoai;

public final class LogSanitizer {

    static final int MAX_LOG_BODY_LENGTH = 500;

    private LogSanitizer() {}

    static String truncate(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        if (value.length() <= MAX_LOG_BODY_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_LOG_BODY_LENGTH) + "...";
    }
}
