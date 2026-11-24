package con.aidenjia.digitalhuman.conversation.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;

/** 演示旁路已注入认证时返回 {@code null}，避免 Resource Server 再对固定串做 JWKS 验签。 */
public class DemoAwareBearerTokenResolver implements BearerTokenResolver {

    private final BearerTokenResolver delegate = new DefaultBearerTokenResolver();

    @Override
    public String resolve(HttpServletRequest request) {
        if (DemoBearerAuthenticationFilter.isDemoAuthenticated(request)) {
            return null;
        }
        return delegate.resolve(request);
    }
}
