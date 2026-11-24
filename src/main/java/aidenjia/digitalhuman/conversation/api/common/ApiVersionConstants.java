package con.aidenjia.digitalhuman.conversation.api.common;

import java.util.Set;

/** Header 版本约定（Boot 3.5 duplicate mapping 方案），与 {@code api-versioning.mdc} 一致。 */
public final class ApiVersionConstants {

    public static final String REQUEST_HEADER = "X-Api-Version";
    public static final String RESPONSE_SUPPORTED_HEADER = "api-supported-versions";

    public static final String VERSION_1 = "1";
    public static final String VERSION_2 = "2";
    public static final String DEFAULT_VERSION = VERSION_1;

    /** {@code @PostMapping(headers = ...)} 条件：仅匹配 v2。 */
    public static final String HEADER_CONDITION_V2 = REQUEST_HEADER + "=" + VERSION_2;

    public static final Set<String> SUPPORTED_VERSIONS = Set.of(VERSION_1, VERSION_2);

    /** 已声明多版本并存的路径（{@code servletPath}，当前无多版本端点）。 */
    public static final Set<String> MULTI_VERSION_PATHS = Set.of();

    public static final String MULTI_VERSION_SUPPORTED_HEADER_VALUE = VERSION_1 + "," + VERSION_2;

    private ApiVersionConstants() {}
}
