package con.aidenjia.digitalhuman.conversation.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/** API 鉴权配置：OpenID Connect JWT（本地 JWKS 验签 + {@code iss} + {@code exp}）。 */
@ConfigurationProperties(prefix = "api.security")
public class ApiSecurityProperties {

    /** 主 Issuer（须与 token {@code iss} 逐字节一致，含尾 {@code /}）。 */
    private String issuerUri = "";

    /** JWKS URI；可空，由 {@link #issuerUri} discovery 解析。 */
    private String jwkSetUri = "";

    /** 额外合法 Issuer（如 Prod {@code https://issuer-prod.example.test/}）。支持 YAML 列表或逗号分隔 环境变量。 */
    private List<String> additionalIssuerUris = new ArrayList<>();

    private int clockSkewSeconds = 60;

    /**
     * 演示旁路：为 true 时，白名单路径可用 {@link #demoToken} 跳过 JWKS（agents、rtc/credentials、webrtc 信令；WS 见握手）。
     */
    private boolean demoTokenEnabled = false;

    /** 演示固定 Bearer（明文仅用于本地/dev 演示；prod 须关闭旁路）。 */
    private String demoToken = "";

    /** 演示合成 JWT {@code sub}，用于 WebRTC 票据的用户归属校验。 */
    private String demoUserId = "demo-ue5";

    public String getIssuerUri() {
        return issuerUri;
    }

    public void setIssuerUri(String issuerUri) {
        this.issuerUri = issuerUri;
    }

    public String getJwkSetUri() {
        return jwkSetUri;
    }

    public void setJwkSetUri(String jwkSetUri) {
        this.jwkSetUri = jwkSetUri;
    }

    public List<String> getAdditionalIssuerUris() {
        return additionalIssuerUris;
    }

    public void setAdditionalIssuerUris(List<String> additionalIssuerUris) {
        this.additionalIssuerUris =
                additionalIssuerUris == null
                        ? new ArrayList<>()
                        : new ArrayList<>(additionalIssuerUris);
    }

    /** 支持 {@code API_SECURITY_ADDITIONAL_ISSUER_URIS=https://a/,https://b/} 单环境变量绑定。 */
    public void setAdditionalIssuerUris(String commaSeparated) {
        if (!StringUtils.hasText(commaSeparated)) {
            this.additionalIssuerUris = new ArrayList<>();
            return;
        }
        this.additionalIssuerUris =
                Arrays.stream(commaSeparated.split(","))
                        .map(String::trim)
                        .filter(StringUtils::hasText)
                        .toList();
    }

    public int getClockSkewSeconds() {
        return clockSkewSeconds;
    }

    public void setClockSkewSeconds(int clockSkewSeconds) {
        this.clockSkewSeconds = clockSkewSeconds;
    }

    public List<String> resolveValidIssuers() {
        List<String> issuers = new ArrayList<>();
        if (StringUtils.hasText(issuerUri)) {
            issuers.add(issuerUri.trim());
        }
        for (String additional : additionalIssuerUris) {
            if (StringUtils.hasText(additional)) {
                issuers.add(additional.trim());
            }
        }
        return List.copyOf(issuers);
    }

    public boolean isDemoTokenEnabled() {
        return demoTokenEnabled;
    }

    public void setDemoTokenEnabled(boolean demoTokenEnabled) {
        this.demoTokenEnabled = demoTokenEnabled;
    }

    public String getDemoToken() {
        return demoToken;
    }

    public void setDemoToken(String demoToken) {
        this.demoToken = demoToken == null ? "" : demoToken;
    }

    public String getDemoUserId() {
        return demoUserId;
    }

    public void setDemoUserId(String demoUserId) {
        this.demoUserId = demoUserId == null ? "" : demoUserId;
    }
}
