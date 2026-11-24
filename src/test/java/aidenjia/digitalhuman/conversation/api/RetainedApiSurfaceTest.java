package con.aidenjia.digitalhuman.conversation.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.TreeSet;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@SpringBootTest
@ActiveProfiles("test")
class RetainedApiSurfaceTest {
    @Autowired private RequestMappingHandlerMapping requestMappingHandlerMapping;
    @Autowired private ApplicationContext context;

    @Test
    void exposesOnlyConversationBusinessRoutesWithoutDatabase() {
        Set<String> actual = new TreeSet<>();
        requestMappingHandlerMapping
                .getHandlerMethods()
                .forEach(
                        (mapping, handler) -> {
                            if (handler.getBeanType()
                                    .getPackageName()
                                    .startsWith("con.aidenjia.digitalhuman.conversation.api")) {
                                mapping.getPatternValues().stream()
                                        .filter(path -> path.startsWith("/api/"))
                                        .forEach(
                                                path ->
                                                        mapping.getMethodsCondition()
                                                                .getMethods()
                                                                .forEach(
                                                                        method ->
                                                                                actual.add(
                                                                                        method
                                                                                                        .name()
                                                                                                + " "
                                                                                                + path)));
                            }
                        });
        assertThat(actual)
                .isEqualTo(
                        Set.of(
                                "POST /api/convoai/agents",
                                "DELETE /api/convoai/agents",
                                "POST /api/rtc/credentials",
                                "POST /api/bailian/realtime/webrtc"));
        assertThat(context.getBeansOfType(DataSource.class)).isEmpty();
        assertThat(
                        context.getBean(
                                con.aidenjia.digitalhuman.conversation.trtc.support
                                        .ConvoaiAgentOwnershipStore.class))
                .isInstanceOf(
                        con.aidenjia.digitalhuman.conversation.trtc.support
                                .InMemoryConvoaiAgentOwnershipStore.class);
        assertThat(
                        context.getBean(
                                con.aidenjia.digitalhuman.conversation.bailian.webrtc
                                        .BailianWebRtcTicketStore.class))
                .isInstanceOf(
                        con.aidenjia.digitalhuman.conversation.bailian.webrtc
                                .InMemoryBailianWebRtcTicketStore.class);
    }
}
