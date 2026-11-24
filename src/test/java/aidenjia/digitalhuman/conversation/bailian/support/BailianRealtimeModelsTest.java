package con.aidenjia.digitalhuman.conversation.bailian.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BailianRealtimeModelsTest {

    @Test
    @DisplayName("未传回落 flash；仅 flash 与 plus 合法")
    void allowlist() {
        assertThat(BailianRealtimeModels.resolve(null)).isEqualTo(BailianRealtimeModels.FLASH);
        assertThat(BailianRealtimeModels.resolve("  ")).isEqualTo(BailianRealtimeModels.FLASH);
        assertThat(BailianRealtimeModels.isAllowed(BailianRealtimeModels.FLASH)).isTrue();
        assertThat(BailianRealtimeModels.isAllowed(BailianRealtimeModels.PLUS)).isTrue();
        assertThat(BailianRealtimeModels.isAllowed(null)).isFalse();
        assertThat(BailianRealtimeModels.isAllowed("")).isFalse();
        assertThat(BailianRealtimeModels.isAllowed("qwen-audio-3.0-realtime")).isFalse();
        assertThat(BailianRealtimeModels.isAllowed("qwen-omni-turbo-realtime")).isFalse();
    }
}
