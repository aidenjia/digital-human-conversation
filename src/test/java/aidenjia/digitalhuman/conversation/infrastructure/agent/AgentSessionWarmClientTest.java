package con.aidenjia.digitalhuman.conversation.infrastructure.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AgentSessionWarmClientTest {

    @Test
    @DisplayName("显式 url 优先于 llm.url 推导")
    void explicitUrlWins() {
        assertEquals(
                "https://agent.example/v1/sessions/warm",
                AgentSessionWarmClient.resolveWarmUrl(
                        "https://agent.example/v1/sessions/warm",
                        "https://agent.example/v1/chat/completions"));
    }

    @Test
    @DisplayName("从 llm.url 推导 warm 路径")
    void derivesFromLlmUrl() {
        assertEquals(
                "https://agent.example.test/v1/sessions/warm",
                AgentSessionWarmClient.resolveWarmUrl(
                        null, "https://agent.example.test/v1/chat/completions"));
    }

    @Test
    @DisplayName("无法推导时返回 null")
    void unresolvedReturnsNull() {
        assertNull(AgentSessionWarmClient.resolveWarmUrl(null, "https://example.com/llm"));
        assertNull(AgentSessionWarmClient.resolveWarmUrl("  ", null));
        assertNull(AgentSessionWarmClient.resolveWarmUrl(null, null));
    }
}
