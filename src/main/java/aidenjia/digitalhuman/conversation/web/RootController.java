package con.aidenjia.digitalhuman.conversation.web;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {

    @Nullable private final BuildProperties buildProperties;
    private final String applicationName;

    public RootController(
            @Nullable BuildProperties buildProperties,
            @Value("${spring.application.name}") String applicationName) {
        this.buildProperties = buildProperties;
        this.applicationName = applicationName;
    }

    @GetMapping("/")
    public Map<String, String> root() {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("name", applicationName);
        body.put("version", buildProperties != null ? buildProperties.getVersion() : "unknown");
        return body;
    }
}
