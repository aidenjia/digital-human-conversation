package con.aidenjia.digitalhuman.conversation.exception;

public class ConfigurationException extends ApiException {

    public ConfigurationException(String message) {
        super(503, message);
    }
}
