package con.aidenjia.digitalhuman.conversation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "digital-human.avatar-decision")
public class AvatarDecisionProperties {
    private boolean enabled = true;
    private String apiKey = "";
    private String baseUrl = "";
    private String model = "qwen3.7-flash";
    private int timeoutMs = 1200;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean value) {
        enabled = value;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String value) {
        apiKey = value;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String value) {
        baseUrl = value;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String value) {
        model = value;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int value) {
        timeoutMs = value;
    }
}
