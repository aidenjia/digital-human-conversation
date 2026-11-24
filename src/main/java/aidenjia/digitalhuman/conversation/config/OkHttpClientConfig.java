package con.aidenjia.digitalhuman.conversation.config;

import java.util.concurrent.TimeUnit;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 共享 OkHttp 客户端：join / leave（Agora）与 session warm（agent）复用同一连接池。
 *
 * <p>场景级超时通过 {@code shared.newBuilder().callTimeout(...).build()} 覆盖；派生客户端共享连接池与 Dispatcher。
 */
@Configuration
@EnableConfigurationProperties(OkHttpProperties.class)
public class OkHttpClientConfig {

    @Bean
    public OkHttpClient okHttpClient(OkHttpProperties props) {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(props.getMaxRequests());
        dispatcher.setMaxRequestsPerHost(props.getMaxRequestsPerHost());

        ConnectionPool pool =
                new ConnectionPool(
                        props.getMaxIdleConnections(),
                        props.getKeepAliveMinutes(),
                        TimeUnit.MINUTES);

        return new OkHttpClient.Builder()
                .dispatcher(dispatcher)
                .connectionPool(pool)
                .connectTimeout(props.getConnectTimeoutMs(), TimeUnit.MILLISECONDS)
                .readTimeout(props.getReadTimeoutMs(), TimeUnit.MILLISECONDS)
                .writeTimeout(props.getWriteTimeoutMs(), TimeUnit.MILLISECONDS)
                .build();
    }
}
