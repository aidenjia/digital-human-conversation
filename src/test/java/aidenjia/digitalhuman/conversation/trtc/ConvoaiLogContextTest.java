package con.aidenjia.digitalhuman.conversation.trtc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import con.aidenjia.digitalhuman.conversation.trtc.support.ConvoaiLogContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class ConvoaiLogContextTest {

    @AfterEach
    void tearDown() {
        ConvoaiLogContext.clear();
    }

    @Test
    @DisplayName("bindJoinPreview 写入 deviceId 与 digitalHumanType")
    void bindJoinPreviewSetsMdc() {
        ConvoaiLogContext.bindJoinPreview("device-1", "UE5");

        assertEquals("device-1", MDC.get(ConvoaiLogContext.MDC_DEVICE_ID));
        assertEquals("UE5", MDC.get(ConvoaiLogContext.MDC_DIGITAL_HUMAN_TYPE));
    }

    @Test
    @DisplayName("enrichAfterJoin 补充 channel userId agentId")
    void enrichAfterJoinSetsMdc() {
        ConvoaiLogContext.enrichAfterJoin("ch-1", "device-1001", "agent-9");

        assertEquals("ch-1", MDC.get(ConvoaiLogContext.MDC_CHANNEL));
        assertEquals("device-1001", MDC.get(ConvoaiLogContext.MDC_USER_ID));
        assertEquals("agent-9", MDC.get(ConvoaiLogContext.MDC_AGENT_ID));
    }

    @Test
    @DisplayName("clear 移除所有 MDC 键")
    void clearRemovesMdc() {
        ConvoaiLogContext.bindLeave("agent-1");
        ConvoaiLogContext.clear();

        assertNull(MDC.get(ConvoaiLogContext.MDC_AGENT_ID));
    }
}
