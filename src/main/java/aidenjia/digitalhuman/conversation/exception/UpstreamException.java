package con.aidenjia.digitalhuman.conversation.exception;

public class UpstreamException extends ApiException {

    public UpstreamException(String message) {
        super(502, message);
    }

    public UpstreamException(String message, Throwable cause) {
        super(502, message, cause);
    }
}
