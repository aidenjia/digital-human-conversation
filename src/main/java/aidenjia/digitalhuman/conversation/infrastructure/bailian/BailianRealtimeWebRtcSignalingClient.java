package con.aidenjia.digitalhuman.conversation.infrastructure.bailian;

import con.aidenjia.digitalhuman.conversation.bailian.support.BailianRealtimeModels;
import con.aidenjia.digitalhuman.conversation.bailian.support.BailianRealtimeUrls;
import con.aidenjia.digitalhuman.conversation.config.BailianRealtimeProperties;
import con.aidenjia.digitalhuman.conversation.exception.ConfigurationException;
import con.aidenjia.digitalhuman.conversation.exception.UpstreamException;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 用永久 Key 向百炼交换 WebRTC Answer SDP。 */
@Component
public class BailianRealtimeWebRtcSignalingClient {

    private static final Logger log =
            LoggerFactory.getLogger(BailianRealtimeWebRtcSignalingClient.class);

    private static final MediaType SDP = MediaType.parse("application/sdp");

    private final OkHttpClient httpClient;
    private final BailianRealtimeProperties properties;

    public BailianRealtimeWebRtcSignalingClient(
            OkHttpClient httpClient, BailianRealtimeProperties properties) {
        this.httpClient = httpClient;
        this.properties = properties;
    }

    public String exchange(String model, String offerSdp) {
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new ConfigurationException("bailian.realtime.api-key is not configured");
        }
        if (!StringUtils.hasText(properties.getWorkspaceId())) {
            throw new ConfigurationException("bailian.realtime.workspace-id is not configured");
        }
        if (!BailianRealtimeModels.isAllowed(model)) {
            throw new IllegalArgumentException("unsupported bailian realtime model");
        }
        String url = BailianRealtimeUrls.buildWebRtcSignalUrl(properties.getWorkspaceId(), model);
        Request request =
                new Request.Builder()
                        .url(url)
                        .header("Authorization", "Bearer " + properties.getApiKey().trim())
                        .post(RequestBody.create(offerSdp, SDP))
                        .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                var failure = BailianSignalingFailure.read(response, properties.getApiKey());
                log.warn(
                        "Bailian WebRTC signaling failed httpStatus={} upstreamCode={} reason={} requestId={} workspaceId={} model={}",
                        response.code(),
                        failure.code(),
                        failure.reason(),
                        failure.requestId(),
                        BailianSignalingFailure.safeIdentifier(
                                properties.getWorkspaceId(), properties.getApiKey()),
                        model);
                throw new UpstreamException("bailian webrtc signaling failed");
            }
            String body = response.body() == null ? "" : response.body().string();
            if (!StringUtils.hasText(body)) {
                throw new UpstreamException("bailian webrtc signaling returned empty answer");
            }
            return body;
        } catch (UpstreamException ex) {
            throw ex;
        } catch (IOException ex) {
            log.warn("Bailian WebRTC signaling io failure");
            throw new UpstreamException("bailian webrtc signaling failed", ex);
        }
    }
}
