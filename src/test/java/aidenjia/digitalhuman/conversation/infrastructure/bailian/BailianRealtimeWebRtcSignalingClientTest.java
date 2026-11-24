package con.aidenjia.digitalhuman.conversation.infrastructure.bailian;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import con.aidenjia.digitalhuman.conversation.bailian.support.BailianRealtimeModels;
import con.aidenjia.digitalhuman.conversation.config.BailianRealtimeProperties;
import con.aidenjia.digitalhuman.conversation.exception.UpstreamException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class BailianRealtimeWebRtcSignalingClientTest {
    private static final String KEY = "sk-private-signaling-test-key";
    private static final String OFFER = "v=0 private-offer-must-not-be-logged";
    private static final String REQUEST_ID = "a7315ddd-f2c1-4426-b282-020a684b8a74";

    private BailianRealtimeWebRtcSignalingClient client(int status, String body, String requestId) {
        var properties = new BailianRealtimeProperties();
        properties.setApiKey(KEY);
        properties.setWorkspaceId("workspace-test");
        var http =
                new OkHttpClient.Builder()
                        .addInterceptor(
                                chain -> {
                                    var response =
                                            new Response.Builder()
                                                    .request(chain.request())
                                                    .protocol(Protocol.HTTP_1_1)
                                                    .code(status)
                                                    .message("test")
                                                    .body(
                                                            ResponseBody.create(
                                                                    body,
                                                                    MediaType.get(
                                                                            "application/json")));
                                    if (requestId != null)
                                        response.header("x-request-id", requestId);
                                    return response.build();
                                })
                        .build();
        return new BailianRealtimeWebRtcSignalingClient(http, properties);
    }

    @Test
    void recordsAccountErrorCodeAndRequestIdWithoutRawBodyOrCredentials(CapturedOutput output) {
        var client =
                client(
                        403,
                        "{\"code\":\"Arrearage\",\"message\":\"Access denied, please make sure your account is in good standing. "
                                + KEY
                                + "\",\"request_id\":\""
                                + REQUEST_ID
                                + "\"}",
                        null);
        assertThatThrownBy(() -> client.exchange(BailianRealtimeModels.FLASH, OFFER))
                .isInstanceOf(UpstreamException.class)
                .hasMessage("bailian webrtc signaling failed");
        assertThat(output)
                .contains(
                        "httpStatus=403",
                        "upstreamCode=Arrearage",
                        "reason=account_status",
                        "requestId=" + REQUEST_ID,
                        "workspaceId=workspace-test",
                        "model=" + BailianRealtimeModels.FLASH);
        assertThat(output).doesNotContain(KEY, OFFER, "good standing", "Authorization");
    }

    @Test
    void recordsNestedModelErrorAndFallsBackToHeaderRequestId(CapturedOutput output) {
        var client =
                client(
                        403,
                        "{\"error\":{\"code\":\"Model.AccessDenied\",\"message\":\"Model access denied.\"}}",
                        REQUEST_ID);
        assertThatThrownBy(() -> client.exchange(BailianRealtimeModels.PLUS, OFFER))
                .isInstanceOf(UpstreamException.class);
        assertThat(output)
                .contains(
                        "upstreamCode=Model.AccessDenied",
                        "reason=model_access",
                        "requestId=" + REQUEST_ID);
    }

    @Test
    void malformedOrSecretContainingIdentifiersNeverEnterLogs(CapturedOutput output) {
        for (String body :
                new String[] {
                    "",
                    "x".repeat(17_000) + KEY,
                    "<html>" + KEY + OFFER + "</html>",
                    "{\"code\":\""
                            + KEY
                            + "\",\"request_id\":\""
                            + KEY
                            + "\",\"message\":\""
                            + OFFER
                            + "\"}"
                }) {
            var client = client(403, body, REQUEST_ID);
            assertThatThrownBy(() -> client.exchange(BailianRealtimeModels.FLASH, OFFER))
                    .isInstanceOf(UpstreamException.class);
        }
        assertThat(output).contains("upstreamCode=unknown", "requestId=" + REQUEST_ID);
        assertThat(output).doesNotContain(KEY, OFFER, "<html>");
    }

    @Test
    void distinguishesWorkspaceAndServiceEntitlementHints(CapturedOutput output) {
        for (String code : new String[] {"Workspace.AccessDenied", "AccessDenied.Unpurchased"}) {
            var client = client(403, "{\"code\":\"" + code + "\"}", REQUEST_ID);
            assertThatThrownBy(() -> client.exchange(BailianRealtimeModels.FLASH, OFFER))
                    .isInstanceOf(UpstreamException.class);
        }
        assertThat(output).contains("reason=workspace_access", "reason=service_entitlement");
    }

    @Test
    void successfulAnswerStillReturnsUnchanged(CapturedOutput output) {
        assertThat(client(200, "v=answer", null).exchange(BailianRealtimeModels.FLASH, OFFER))
                .isEqualTo("v=answer");
        assertThat(output).doesNotContain("signaling failed", KEY, OFFER);
    }
}
