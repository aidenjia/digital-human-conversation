package con.aidenjia.digitalhuman.conversation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 腾讯云账号级 API 密钥：供 ConvoAI 内置 Tencent TTS 使用。 */
@ConfigurationProperties(prefix = "tencent.credentials")
public class TencentCloudCredentialsProperties {

    private String secretId = "";

    private String secretKey = "";

    public String getSecretId() {
        return secretId;
    }

    public void setSecretId(String secretId) {
        this.secretId = secretId;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public boolean isConfigured() {
        return secretId != null && !secretId.isBlank() && secretKey != null && !secretKey.isBlank();
    }
}
