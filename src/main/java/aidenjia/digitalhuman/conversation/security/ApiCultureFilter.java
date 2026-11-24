package con.aidenjia.digitalhuman.conversation.security;

import con.aidenjia.digitalhuman.conversation.api.common.ApiPaths;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public class ApiCultureFilter extends OncePerRequestFilter {

    private static final Set<String> SUPPORTED_CULTURES = Set.of("en", "zh");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !path.startsWith(ApiPaths.API_PREFIX);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String culture = resolveCulture(request.getHeader(ApiCultureConstants.REQUEST_HEADER));
        request.setAttribute(ApiCultureConstants.REQUEST_ATTRIBUTE, culture);
        response.setHeader(ApiCultureConstants.RESPONSE_HEADER, culture);
        filterChain.doFilter(request, response);
    }

    private static String resolveCulture(String requested) {
        if (!StringUtils.hasText(requested)) {
            return ApiCultureConstants.DEFAULT_CULTURE;
        }
        String normalized = requested.trim().toLowerCase();
        return SUPPORTED_CULTURES.contains(normalized)
                ? normalized
                : ApiCultureConstants.DEFAULT_CULTURE;
    }
}
