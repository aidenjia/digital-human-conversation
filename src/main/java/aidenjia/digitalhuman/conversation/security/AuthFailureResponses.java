package con.aidenjia.digitalhuman.conversation.security;

import con.aidenjia.digitalhuman.conversation.api.common.ApiResponse;
import con.aidenjia.digitalhuman.conversation.api.common.ApiResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.util.StringUtils;

/** 鉴权失败统一信封与指南 §5.2 文案（en/zh 按 {@link ApiCultureConstants#REQUEST_ATTRIBUTE}）。 */
public final class AuthFailureResponses {

    public static final int CODE_UNAUTHENTICATED = 401;
    public static final int CODE_INVALID_TOKEN = 4010;
    public static final int CODE_ACCESS_DENIED = 4030;

    private AuthFailureResponses() {}

    public static void write(
            HttpServletRequest request,
            HttpServletResponse response,
            ApiResponseWriter writer,
            int code)
            throws IOException {
        int httpStatus = code == CODE_ACCESS_DENIED ? HttpServletResponse.SC_FORBIDDEN : 401;
        writer.write(response, httpStatus, ApiResponse.fail(code, messageFor(request, code)));
    }

    public static String messageFor(HttpServletRequest request, int code) {
        return messageForCulture(resolveCulture(request), code);
    }

    static String messageForCulture(String culture, int code) {
        boolean zh = "zh".equalsIgnoreCase(culture);
        return switch (code) {
            case CODE_UNAUTHENTICATED -> zh ? "无法鉴权访问" : "Cannot authenticate";
            case CODE_INVALID_TOKEN -> zh ? "访问令牌已失效" : "Token is invalid";
            case CODE_ACCESS_DENIED -> zh ? "无访问权限" : "Access is denied";
            default -> zh ? "无法鉴权访问" : "Cannot authenticate";
        };
    }

    private static String resolveCulture(HttpServletRequest request) {
        if (request == null) {
            return ApiCultureConstants.DEFAULT_CULTURE;
        }
        Object attribute = request.getAttribute(ApiCultureConstants.REQUEST_ATTRIBUTE);
        if (attribute instanceof String culture && StringUtils.hasText(culture)) {
            return culture;
        }
        return ApiCultureConstants.DEFAULT_CULTURE;
    }
}
