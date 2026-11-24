package con.aidenjia.digitalhuman.conversation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 百炼 Realtime（Qwen-Audio）配置：WebRTC 信令与控制会话。
 *
 * <p>永久 API Key 仅服务端使用。
 */
@ConfigurationProperties(prefix = "bailian.realtime")
public class BailianRealtimeProperties {

    /** 永久 DashScope API Key（环境变量 {@code DASHSCOPE_API_KEY}）。 */
    private String apiKey = "";

    /** 业务空间 ID（拼 WebRTC SDP 信令 URL）。 */
    private String workspaceId = "";

    /** 连上游 session 就绪后是否自动口播欢迎语。 */
    private boolean greetingEnabled = true;

    /** 欢迎语文案（固定口播，不改写）。 */
    private String greetingText = "你好，有什么想聊的吗";

    /** 轮次检测：{@code smart_turn}（语义、更抗误打断）或 {@code server_vad}（快打断）。 */
    private String turnDetectionType = "smart_turn";

    /** 历史 QA 轮数上限（1–50）。 */
    private int maxHistoryTurns = 30;

    /** 是否开启百炼联网搜索。 */
    private boolean enableSearch = false;

    /** 客户端↔本服务心跳间隔（秒）。 */
    private int heartbeatIntervalSeconds = 15;

    /** 无心跳空闲超时（秒），超时关闭控制连接。 */
    private int idleTimeoutSeconds = 60;

    /** 优雅下线等待客户端反应的宽限（秒）。 */
    private int drainGraceSeconds = 15;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public boolean isGreetingEnabled() {
        return greetingEnabled;
    }

    public void setGreetingEnabled(boolean greetingEnabled) {
        this.greetingEnabled = greetingEnabled;
    }

    public String getGreetingText() {
        return greetingText;
    }

    public void setGreetingText(String greetingText) {
        this.greetingText = greetingText;
    }

    public String getTurnDetectionType() {
        return turnDetectionType;
    }

    public void setTurnDetectionType(String turnDetectionType) {
        this.turnDetectionType = turnDetectionType;
    }

    public int getMaxHistoryTurns() {
        return maxHistoryTurns;
    }

    public void setMaxHistoryTurns(int maxHistoryTurns) {
        this.maxHistoryTurns = maxHistoryTurns;
    }

    public boolean isEnableSearch() {
        return enableSearch;
    }

    public void setEnableSearch(boolean enableSearch) {
        this.enableSearch = enableSearch;
    }

    public int getHeartbeatIntervalSeconds() {
        return heartbeatIntervalSeconds;
    }

    public void setHeartbeatIntervalSeconds(int heartbeatIntervalSeconds) {
        this.heartbeatIntervalSeconds = heartbeatIntervalSeconds;
    }

    public int getIdleTimeoutSeconds() {
        return idleTimeoutSeconds;
    }

    public void setIdleTimeoutSeconds(int idleTimeoutSeconds) {
        this.idleTimeoutSeconds = idleTimeoutSeconds;
    }

    public int getDrainGraceSeconds() {
        return drainGraceSeconds;
    }

    public void setDrainGraceSeconds(int drainGraceSeconds) {
        this.drainGraceSeconds = drainGraceSeconds;
    }
}
