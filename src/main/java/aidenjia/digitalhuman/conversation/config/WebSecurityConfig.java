package con.aidenjia.digitalhuman.conversation.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import con.aidenjia.digitalhuman.conversation.api.common.ApiPaths;
import con.aidenjia.digitalhuman.conversation.api.common.ApiResponseWriter;
import con.aidenjia.digitalhuman.conversation.security.ApiCultureFilter;
import con.aidenjia.digitalhuman.conversation.security.ApiVersionFilter;
import con.aidenjia.digitalhuman.conversation.security.ConvoaiMdcFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class WebSecurityConfig {

    @Bean
    public FilterRegistrationBean<ApiCultureFilter> apiCultureFilter() {
        FilterRegistrationBean<ApiCultureFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ApiCultureFilter());
        registration.addUrlPatterns(ApiPaths.API_PREFIX + "*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 5);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<ApiVersionFilter> apiVersionFilter(
            ApiResponseWriter apiResponseWriter) {
        FilterRegistrationBean<ApiVersionFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ApiVersionFilter(apiResponseWriter));
        registration.addUrlPatterns(ApiPaths.API_PREFIX + "*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 7);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<ConvoaiMdcFilter> convoaiMdcFilter(ObjectMapper objectMapper) {
        FilterRegistrationBean<ConvoaiMdcFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ConvoaiMdcFilter(objectMapper));
        registration.addUrlPatterns(ApiPaths.CONVOAI_PREFIX + "*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 12);
        return registration;
    }
}
