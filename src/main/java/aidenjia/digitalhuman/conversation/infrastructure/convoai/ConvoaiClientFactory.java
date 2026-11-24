package con.aidenjia.digitalhuman.conversation.infrastructure.convoai;

import con.aidenjia.digitalhuman.conversation.config.ConvoaiProperties;
import con.aidenjia.digitalhuman.conversation.exception.ConfigurationException;
import org.springframework.stereotype.Component;

/** ConvoAI 配置校验与访问（join / leave 均走 {@link AgoraRestClient}，不再创建 SDK Client）。 */
@Component
public class ConvoaiClientFactory {

    private final ConvoaiProperties properties;

    public ConvoaiClientFactory(ConvoaiProperties properties) {
        this.properties = properties;
    }

    public ConvoaiProperties getProperties() {
        return properties;
    }

    /** 校验 Agora REST（join / leave）所需的最小配置。 */
    public void validateForJoin() {
        if (isBlank(properties.getAppId())) {
            throw new ConfigurationException("ConvoAI app id is not configured");
        }
        if (isBlank(properties.getUsername())) {
            throw new ConfigurationException("ConvoAI basic auth username is not configured");
        }
        if (isBlank(properties.getPassword())) {
            throw new ConfigurationException("ConvoAI basic auth password is not configured");
        }
        if (properties.getRtc().isTokenAuthEnabled()
                && isBlank(properties.getRtc().getAppCertificate())) {
            throw new ConfigurationException("ConvoAI RTC app certificate is not configured");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
