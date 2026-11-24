package con.aidenjia.digitalhuman.conversation.bailian.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BailianRealtimeUrlsTest {

    @Test
    @DisplayName("WebRTC 信令地址使用同一 workspace 与模型")
    void buildWebRtcSignalUrlUsesSelectedModel() {
        assertThat(BailianRealtimeUrls.buildWebRtcSignalUrl("ws-abc", BailianRealtimeModels.FLASH))
                .isEqualTo(
                        "https://ws-abc.cn-beijing.maas.aliyuncs.com/api/v1/webrtc/realtime?model=qwen-audio-3.0-realtime-flash");
    }
}
