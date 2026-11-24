package con.aidenjia.digitalhuman.conversation.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 启动时校验 {@code api.security} 必填项（fail-fast）。 */
@Component
public class ApiSecurityStartupValidator {

    private final ApiSecurityProperties apiSecurity;

    public ApiSecurityStartupValidator(ApiSecurityProperties apiSecurity) {
        this.apiSecurity = apiSecurity;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validateOnReady() {
        if (!StringUtils.hasText(apiSecurity.getIssuerUri())
                && !StringUtils.hasText(apiSecurity.getJwkSetUri())) {
            throw new IllegalStateException(
                    "api.security.issuer-uri or api.security.jwk-set-uri is required");
        }
        if (apiSecurity.isDemoTokenEnabled()) {
            if (!StringUtils.hasText(apiSecurity.getDemoToken())) {
                throw new IllegalStateException(
                        "api.security.demo-token is required when demo-token-enabled=true");
            }
            if (!StringUtils.hasText(apiSecurity.getDemoUserId())) {
                throw new IllegalStateException(
                        "api.security.demo-user-id is required when demo-token-enabled=true");
            }
        }
    }
}
