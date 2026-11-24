package con.aidenjia.digitalhuman.conversation.api.convoai;

import con.aidenjia.digitalhuman.conversation.api.common.ApiResponse;
import con.aidenjia.digitalhuman.conversation.api.convoai.dto.ConvoaiJoinCredentials;
import con.aidenjia.digitalhuman.conversation.api.convoai.dto.JoinConvoaiRequest;
import con.aidenjia.digitalhuman.conversation.api.convoai.dto.LeaveConvoaiRequest;
import con.aidenjia.digitalhuman.conversation.api.convoai.dto.LeaveConvoaiResult;
import con.aidenjia.digitalhuman.conversation.security.CurrentUserIdAccessor;
import con.aidenjia.digitalhuman.conversation.trtc.ConvoaiService;
import con.aidenjia.digitalhuman.conversation.trtc.command.JoinConvoaiCommand;
import con.aidenjia.digitalhuman.conversation.trtc.command.LeaveConvoaiCommand;
import con.aidenjia.digitalhuman.conversation.trtc.support.ConvoaiJoinGuard;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 对话式 AI 引擎 REST 接口（封装声网 ConvoAI SDK）。
 *
 * <p>鉴权：{@code Authorization: Bearer <Users access token>}（OpenID JWT）；userId 取 JWT {@code sub}。
 */
@RestController
@RequestMapping("/api/convoai/agents")
public class ConvoaiController {

    private final ConvoaiService convoaiService;
    private final ConvoaiJoinGuard joinGuard;
    private final CurrentUserIdAccessor currentUserIdAccessor;

    public ConvoaiController(
            ConvoaiService convoaiService,
            ConvoaiJoinGuard joinGuard,
            CurrentUserIdAccessor currentUserIdAccessor) {
        this.convoaiService = convoaiService;
        this.joinGuard = joinGuard;
        this.currentUserIdAccessor = currentUserIdAccessor;
    }

    /** 同用户 join 在本进程内互斥；重复请求立即返回 409，不回放成功响应。 */
    @PostMapping
    public ApiResponse<ConvoaiJoinCredentials> join(@Valid @RequestBody JoinConvoaiRequest body) {
        ConvoaiJoinCredentials credentials =
                joinGuard.execute(
                        currentUserIdAccessor.requireUserId(),
                        () ->
                                convoaiService.join(
                                        new JoinConvoaiCommand(
                                                body.deviceId(),
                                                body.digitalHumanType(),
                                                body.timezone())));
        return ApiResponse.ok(credentials);
    }

    @DeleteMapping
    public ApiResponse<LeaveConvoaiResult> leave(@Valid @RequestBody LeaveConvoaiRequest body) {
        LeaveConvoaiResult result = convoaiService.leave(new LeaveConvoaiCommand(body.agentId()));
        return ApiResponse.ok(result);
    }
}
