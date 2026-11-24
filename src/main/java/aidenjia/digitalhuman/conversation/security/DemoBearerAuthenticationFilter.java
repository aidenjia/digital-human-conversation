package con.aidenjia.digitalhuman.conversation.security;

import con.aidenjia.digitalhuman.conversation.config.ApiSecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 演示旁路：开关开启且 Bearer 精确匹配时，对白名单路径注入合成 JWT。
 *
 * <p>白名单：{@code POST|DELETE /api/convoai/agents}、{@code POST /api/rtc/credentials}、{@code POST
 * /api/bailian/realtime/webrtc}。Realtime WS 鉴权在 HandshakeInterceptor，不经本 Filter。
 *
 * <p>配合 {@link DemoAwareBearerTokenResolver} 跳过该请求的 JWKS 验签。
 */
public class DemoBearerAuthenticationFilter extends OncePerRequestFilter {

    static final String DEMO_AUTH_ATTRIBUTE = "demoBearerAuthenticated";

    private static final String AGENTS_PATH = "/api/convoai/agents";
    private static final String RTC_CREDENTIALS_PATH = "/api/rtc/credentials";
    private static final String WEBRTC_SIGNAL_PATH = "/api/bailian/realtime/webrtc";

    private final ApiSecurityProperties apiSecurity;

    public DemoBearerAuthenticationFilter(ApiSecurityProperties apiSecurity) {
        this.apiSecurity = apiSecurity;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (apiSecurity.isDemoTokenEnabled() && matchesDemoBypassPath(request)) {
            String token =
                    BearerTokenSupport.extractBearerToken(
                            request.getHeader(BearerTokenSupport.authorizationHeaderName()));
            if (StringUtils.hasText(token) && token.equals(apiSecurity.getDemoToken())) {
                Jwt jwt = syntheticJwt(token, apiSecurity.getDemoUserId());
                JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
                request.setAttribute(DEMO_AUTH_ATTRIBUTE, Boolean.TRUE);
            }
        }
        filterChain.doFilter(request, response);
    }

    static boolean matchesDemoBypassPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        if (AGENTS_PATH.equals(uri)) {
            return HttpMethod.POST.matches(method) || HttpMethod.DELETE.matches(method);
        }
        if (RTC_CREDENTIALS_PATH.equals(uri) && HttpMethod.POST.matches(method)) {
            return true;
        }
        return WEBRTC_SIGNAL_PATH.equals(uri) && HttpMethod.POST.matches(method);
    }

    static boolean isDemoAuthenticated(HttpServletRequest request) {
        return Boolean.TRUE.equals(request.getAttribute(DEMO_AUTH_ATTRIBUTE));
    }

    private static Jwt syntheticJwt(String token, String userId) {
        Instant now = Instant.now();
        return Jwt.withTokenValue(token)
                .header("alg", "none")
                .subject(userId)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .claim("iss", "demo-bearer")
                .build();
    }
}
