package con.aidenjia.digitalhuman.conversation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 共享 OkHttp 连接池与超时（join / leave / warm 共用同一 {@code OkHttpClient} Bean）。 */
@ConfigurationProperties(prefix = "http.okhttp")
public class OkHttpProperties {

    /** 连接池最大空闲连接数。 */
    private int maxIdleConnections = 20;

    /** 空闲连接保活时长（分钟）。 */
    private long keepAliveMinutes = 5;

    /** 连接超时（毫秒）。 */
    private long connectTimeoutMs = 5_000;

    /** 读超时（毫秒）；Agora join 可能偏慢。 */
    private long readTimeoutMs = 30_000;

    /** 写超时（毫秒）。 */
    private long writeTimeoutMs = 10_000;

    /** 全局最大并发请求。 */
    private int maxRequests = 64;

    /** 单 host 最大并发请求。 */
    private int maxRequestsPerHost = 32;

    public int getMaxIdleConnections() {
        return maxIdleConnections;
    }

    public void setMaxIdleConnections(int maxIdleConnections) {
        this.maxIdleConnections = maxIdleConnections;
    }

    public long getKeepAliveMinutes() {
        return keepAliveMinutes;
    }

    public void setKeepAliveMinutes(long keepAliveMinutes) {
        this.keepAliveMinutes = keepAliveMinutes;
    }

    public long getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(long connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public long getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(long readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public long getWriteTimeoutMs() {
        return writeTimeoutMs;
    }

    public void setWriteTimeoutMs(long writeTimeoutMs) {
        this.writeTimeoutMs = writeTimeoutMs;
    }

    public int getMaxRequests() {
        return maxRequests;
    }

    public void setMaxRequests(int maxRequests) {
        this.maxRequests = maxRequests;
    }

    public int getMaxRequestsPerHost() {
        return maxRequestsPerHost;
    }

    public void setMaxRequestsPerHost(int maxRequestsPerHost) {
        this.maxRequestsPerHost = maxRequestsPerHost;
    }
}
