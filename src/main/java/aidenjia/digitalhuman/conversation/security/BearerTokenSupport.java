package con.aidenjia.digitalhuman.conversation.security;

import org.springframework.util.StringUtils;

public final class BearerTokenSupport {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private BearerTokenSupport() {}

    /** 解析 {@code Authorization: Bearer <token>}；缺头、非 Bearer、空 token 均返回 {@code null}。 */
    public static String extractBearerToken(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }

    /** 是否属于「无法开始认证」的畸形 Bearer（应对指南 code 401）。 */
    public static boolean isMalformedBearerRequest(String authorization) {
        if (!StringUtils.hasText(authorization)) {
            return true;
        }
        if (!authorization.startsWith(BEARER_PREFIX)) {
            return true;
        }
        return authorization.substring(BEARER_PREFIX.length()).trim().isEmpty();
    }

    public static String authorizationHeaderName() {
        return AUTHORIZATION_HEADER;
    }
}
