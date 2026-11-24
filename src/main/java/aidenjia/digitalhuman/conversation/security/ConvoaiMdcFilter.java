package con.aidenjia.digitalhuman.conversation.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import con.aidenjia.digitalhuman.conversation.api.common.ApiPaths;
import con.aidenjia.digitalhuman.conversation.trtc.support.ConvoaiLogContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/** 为 {@code /api/convoai/**} 建立请求级 MDC，便于 Logstash 结构化日志按 deviceId / agentId 检索。 */
public class ConvoaiMdcFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    public ConvoaiMdcFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !path.startsWith(ApiPaths.CONVOAI_PREFIX);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        HttpServletRequest toDispatch = request;
        if (hasJsonBody(request)) {
            CachedBodyHttpServletRequest cached = new CachedBodyHttpServletRequest(request);
            bindFromBody(cached.getCachedBody(), request.getMethod());
            toDispatch = cached;
        }

        try {
            filterChain.doFilter(toDispatch, response);
        } finally {
            ConvoaiLogContext.clear();
        }
    }

    private void bindFromBody(byte[] content, String method) {
        if (content.length == 0) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(content);
            if (HttpMethod.POST.matches(method)) {
                ConvoaiLogContext.bindJoinPreview(
                        text(root, "deviceId"), text(root, "digitalHumanType"));
            } else if (HttpMethod.DELETE.matches(method)) {
                ConvoaiLogContext.bindLeave(text(root, "agentId"));
            }
        } catch (IOException ignored) {
            // 畸形 JSON 由 Controller 校验；不在 Filter 打日志
        }
    }

    private static boolean hasJsonBody(HttpServletRequest request) {
        String method = request.getMethod();
        if (!HttpMethod.POST.matches(method) && !HttpMethod.DELETE.matches(method)) {
            return false;
        }
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().contains("application/json");
    }

    private static String text(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        String value = node.asText(null);
        return StringUtils.hasText(value) ? value : null;
    }
}
