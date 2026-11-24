package con.aidenjia.digitalhuman.conversation.bailian.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import con.aidenjia.digitalhuman.conversation.bailian.support.BailianRealtimeUrls;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RealtimeSessionPromptsTest {

    @Test
    @DisplayName("人设只写黛黛身份，不含打断规则")
    void personaOmitsInterruptRules() {
        assertThat(RealtimePersonaPrompts.PERSONA).contains("你叫黛黛");
        assertThat(RealtimePersonaPrompts.PERSONA).doesNotContain("打断规则");
    }

    @Test
    @DisplayName("规则只约束接话，不含人设身份")
    void rulesOmitPersona() {
        String rules = RealtimeRulesPrompts.rulesBody();
        assertThat(rules).contains("打断规则");
        assertThat(rules).contains("只简短应一声");
        assertThat(rules).doesNotContain("黛黛");
        assertThat(rules).doesNotContain("---");
    }

    @Test
    @DisplayName("规则规定口播硬上限 200 字，人设不重复该限制")
    void rulesCapSpokenLength() {
        assertThat(RealtimeRulesPrompts.rulesBody()).contains("硬上限 200 个字");
        assertThat(RealtimePersonaPrompts.PERSONA).doesNotContain("200 个字");
    }

    @Test
    @DisplayName("session instructions 先 Role 人设，再接规则")
    void instructionsPlacePersonaBeforeRules() {
        String text = BailianRealtimeUrls.fixedInstructions("你是测试人设");
        assertThat(text).startsWith("## Role\n你是测试人设");
        assertThat(text).contains("打断规则");
        assertThat(text.indexOf("打断规则")).isGreaterThan(text.indexOf("你是测试人设"));
    }
}
