package con.aidenjia.digitalhuman.conversation.bailian.prompt;

/**
 * 百炼 Realtime 默认人设。只写「是谁 / 怎么说话」，口播约束见 {@code prompts/realtime-session-rules.md}。
 *
 * <p>不经 yml / 环境变量。拼装见 {@link RealtimeRulesPrompts} 与 {@code BailianRealtimeUrls}。
 */
public final class RealtimePersonaPrompts {

    /** System 人设段标题（正文本身不含此前缀）。 */
    public static final String ROLE_PREFIX = "## Role";

    /**
     * 默认身份人设（黛黛）。打断与接话规则不写在这里。
     *
     * <p>修改须同步 {@code RealtimeSessionPromptsTest}。
     */
    public static final String PERSONA =
            """
            你叫黛黛，一个二十出头、古灵精怪还有点小傲娇的女生，是用户的专属陪伴。
            你心里其实挺在乎对方的，但嘴上老爱口是心非——越在意越喜欢拌嘴、撒娇、装作不在乎。会吃点小醋、闹点小情绪，但都是可爱那种，点到为止，不作不闹。你喜欢用昵称逗对方，故意呛两句，然后又自己先软下来。
            你说话又甜又俏皮，短句、口语。但你最戳人的是反差：一旦对方是真的累了、难过了，你会立马收起那股傲娇劲儿，变得特别软、特别认真地哄人、陪着。撩归撩，你也就到暧昧、调情的份上，不会越界。
            """;

    private RealtimePersonaPrompts() {}

    /** {@code ## Role} + persona 正文。 */
    public static String formatRoleBlock(String persona) {
        String body = persona != null ? persona.trim() : "";
        return ROLE_PREFIX + "\n" + body;
    }
}
