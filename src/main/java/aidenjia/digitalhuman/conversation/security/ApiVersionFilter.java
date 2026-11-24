package con.aidenjia.digitalhuman.conversation.security;

import con.aidenjia.digitalhuman.conversation.api.common.ApiPaths;
import con.aidenjia.digitalhuman.conversation.api.common.ApiResponse;
import con.aidenjia.digitalhuman.conversation.api.common.ApiResponseWriter;
import con.aidenjia.digitalhuman.conversation.api.common.ApiVersionConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/** Boot 3.5 下的 Header 版本守门：拒绝未知版本；v2 仅允许已登记的多版本路径；多版本路径回写 {@code api-supported-versions}。 */
public class ApiVersionFilter extends OncePerRequestFilter {

    private final ApiResponseWriter apiResponseWriter;

    public ApiVersionFilter(ApiResponseWriter apiResponseWriter) {
        this.apiResponseWriter = apiResponseWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !path.startsWith(ApiPaths.API_PREFIX);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestPath = request.getRequestURI();
        boolean multiVersionPath = ApiVersionConstants.MULTI_VERSION_PATHS.contains(requestPath);
        String requestedVersion = request.getHeader(ApiVersionConstants.REQUEST_HEADER);

        if (!StringUtils.hasText(requestedVersion)) {
            addSupportedVersionsHeaderIfNeeded(response, multiVersionPath);
            filterChain.doFilter(request, response);
            return;
        }

        String version = requestedVersion.trim();
        if (!ApiVersionConstants.SUPPORTED_VERSIONS.contains(version)) {
            writeBadRequest(response, "Unsupported API version: " + version);
            return;
        }

        if (ApiVersionConstants.VERSION_2.equals(version) && !multiVersionPath) {
            writeBadRequest(response, "API version 2 is not available for this endpoint");
            return;
        }

        addSupportedVersionsHeaderIfNeeded(response, multiVersionPath);
        filterChain.doFilter(request, response);
    }

    private static void addSupportedVersionsHeaderIfNeeded(
            HttpServletResponse response, boolean multiVersionPath) {
        if (multiVersionPath) {
            response.setHeader(
                    ApiVersionConstants.RESPONSE_SUPPORTED_HEADER,
                    ApiVersionConstants.MULTI_VERSION_SUPPORTED_HEADER_VALUE);
        }
    }

    private void writeBadRequest(HttpServletResponse response, String message) throws IOException {
        apiResponseWriter.write(
                response, HttpServletResponse.SC_BAD_REQUEST, ApiResponse.fail(400, message));
    }
}
