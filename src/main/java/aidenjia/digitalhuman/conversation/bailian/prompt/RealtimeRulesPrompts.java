package con.aidenjia.digitalhuman.conversation.bailian.prompt;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 从 classpath 加载百炼 Realtime 口播规则（类加载时读一次）。不是人设。
 *
 * <p>资源路径：{@code prompts/realtime-session-rules.md}。
 */
public final class RealtimeRulesPrompts {

    static final String RESOURCE = "prompts/realtime-session-rules.md";

    private static final String RULES_BODY = loadBody();

    private RealtimeRulesPrompts() {}

    /** 口播规则正文，启动时加载一次。 */
    public static String rulesBody() {
        return RULES_BODY;
    }

    private static String loadBody() {
        try (InputStream in =
                RealtimeRulesPrompts.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource: " + RESOURCE);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("\r\n", "\n")
                    .replace("\r", "\n")
                    .trim();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + RESOURCE, e);
        }
    }
}
