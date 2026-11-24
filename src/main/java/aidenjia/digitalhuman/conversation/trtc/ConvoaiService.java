package con.aidenjia.digitalhuman.conversation.trtc;

import con.aidenjia.digitalhuman.conversation.api.convoai.dto.ConvoaiJoinCredentials;
import con.aidenjia.digitalhuman.conversation.api.convoai.dto.LeaveConvoaiResult;
import con.aidenjia.digitalhuman.conversation.config.ConvoaiProperties;
import con.aidenjia.digitalhuman.conversation.exception.ApiException;
import con.aidenjia.digitalhuman.conversation.exception.ConfigurationException;
import con.aidenjia.digitalhuman.conversation.exception.UpstreamException;
import con.aidenjia.digitalhuman.conversation.infrastructure.agent.AgentSessionWarmClient;
import con.aidenjia.digitalhuman.conversation.infrastructure.convoai.AgoraRestClient;
import con.aidenjia.digitalhuman.conversation.infrastructure.convoai.ConvoaiClientFactory;
import con.aidenjia.digitalhuman.conversation.security.CurrentUserIdAccessor;
import con.aidenjia.digitalhuman.conversation.trtc.command.JoinConvoaiCommand;
import con.aidenjia.digitalhuman.conversation.trtc.command.LeaveConvoaiCommand;
import con.aidenjia.digitalhuman.conversation.trtc.support.AgoraRtcTokenService;
import con.aidenjia.digitalhuman.conversation.trtc.support.ConvoaiAgentOwnershipStore;
import con.aidenjia.digitalhuman.conversation.trtc.support.ConvoaiJoinBodyAssembler;
import con.aidenjia.digitalhuman.conversation.trtc.support.ConvoaiLogContext;
import con.aidenjia.digitalhuman.conversation.trtc.support.JoinSessionProfile;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 声网对话式 AI 引擎 REST 接口编排。
 *
 * <p>{@link #join} / {@link #leave} 均通过 {@link AgoraRestClient}（共享 OkHttp 连接池）；join 成功后同步
 * best-effort session warm。
 */
@Service
public class ConvoaiService {

    private static final Logger log = LoggerFactory.getLogger(ConvoaiService.class);

    private final AgoraRestClient restClient;
    private final ConvoaiClientFactory clientFactory;
    private final AgoraRtcTokenService rtcTokenService;
    private final ConvoaiJoinBodyAssembler joinBodyAssembler;
    private final AgentSessionWarmClient sessionWarmClient;
    private final CurrentUserIdAccessor currentUserIdAccessor;
    private final ConvoaiAgentOwnershipStore agentOwnershipStore;

    public ConvoaiService(
            AgoraRestClient restClient,
            ConvoaiClientFactory clientFactory,
            AgoraRtcTokenService rtcTokenService,
            ConvoaiJoinBodyAssembler joinBodyAssembler,
            CurrentUserIdAccessor currentUserIdAccessor,
            AgentSessionWarmClient sessionWarmClient,
            ConvoaiAgentOwnershipStore agentOwnershipStore) {
        this.restClient = restClient;
        this.clientFactory = clientFactory;
        this.rtcTokenService = rtcTokenService;
        this.joinBodyAssembler = joinBodyAssembler;
        this.sessionWarmClient = sessionWarmClient;
        this.currentUserIdAccessor = currentUserIdAccessor;
        this.agentOwnershipStore = agentOwnershipStore;
    }

    public ConvoaiJoinCredentials join(JoinConvoaiCommand command) {
        clientFactory.validateForJoin();
        ConvoaiProperties props = clientFactory.getProperties();
        String userId = currentUserIdAccessor.requireUserId();
        String channel = RoomIdGenerator.generate();
        ConvoaiLogContext.enrichAfterJoin(channel, userId, null);
        log.info("ConvoAI join requested");

        validateLlmConfig(props);
        JoinSessionProfile profile = JoinSessionProfile.CONFIGURED;
        String agentToken = rtcTokenService.buildAgentToken(channel);
        String userToken = rtcTokenService.buildUserToken(channel, userId);

        Map<String, Object> body =
                joinBodyAssembler.build(props, command, channel, userId, agentToken, profile);

        AgoraRestClient.JoinResponse response = restClient.join(body);
        ConvoaiLogContext.enrichAfterJoin(channel, userId, response.agentId());
        bindAgentOwnershipBestEffort(response.agentId(), userId);
        warmSessionBestEffort(props, command, channel, userId, profile.replyLanguage());
        return new ConvoaiJoinCredentials(
                props.getAppId(), channel, userToken, userId, response.agentId());
    }

    public LeaveConvoaiResult leave(LeaveConvoaiCommand command) {
        String agentId = command.agentId();
        String userId = currentUserIdAccessor.requireUserId();
        log.info("ConvoAI leave requested");
        assertAgentOwnedByCurrentUser(agentId, userId);
        clientFactory.validateForJoin();
        try {
            restClient.leave(agentId);
        } catch (UpstreamException ex) {
            log.warn("ConvoAI leave failed");
            throw ex;
        }
        agentOwnershipStore.unbind(agentId);
        return new LeaveConvoaiResult(agentId, "STOPPED");
    }

    private void bindAgentOwnershipBestEffort(String agentId, String userId) {
        try {
            agentOwnershipStore.bind(agentId, userId);
        } catch (RuntimeException ex) {
            log.error("ConvoAI agent ownership bind failed agentId={}", agentId, ex);
        }
    }

    private void assertAgentOwnedByCurrentUser(String agentId, String userId) {
        String owner = agentOwnershipStore.findOwner(agentId).orElse(null);
        if (owner == null || !owner.equals(userId)) {
            log.warn(
                    "ConvoAI leave denied agentId={} reason={}",
                    agentId,
                    owner == null ? "ownership_missing" : "ownership_mismatch");
            throw new ApiException(404, "Agent not found");
        }
    }

    private void warmSessionBestEffort(
            ConvoaiProperties props,
            JoinConvoaiCommand command,
            String channel,
            String userId,
            String replyLanguage) {
        ConvoaiProperties.SessionWarm warmCfg = props.getSessionWarm();
        if (warmCfg == null || !warmCfg.isEnabled()) {
            return;
        }
        try {
            sessionWarmClient.warm(
                    userId,
                    channel,
                    command.deviceId(),
                    command.digitalHumanType().name(),
                    command.timezone(),
                    replyLanguage);
        } catch (RuntimeException ex) {
            log.warn("session warm failed reason=unexpected", ex);
        }
    }

    private static void validateLlmConfig(ConvoaiProperties props) {
        if (isBlank(props.getLlmUrl())) {
            throw new ConfigurationException("ConvoAI LLM url is not configured");
        }
        if (isBlank(props.getLlmApiKey())) {
            throw new ConfigurationException("ConvoAI LLM api key is not configured");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
