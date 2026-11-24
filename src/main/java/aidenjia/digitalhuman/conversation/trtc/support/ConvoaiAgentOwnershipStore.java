package con.aidenjia.digitalhuman.conversation.trtc.support;

import java.util.Optional;

/** ConvoAI agent 归属，用于停止会话时验证当前用户。 */
public interface ConvoaiAgentOwnershipStore {

    /** join 成功后登记 agent 归属当前用户。失败由实现吞掉或抛出；Service 层不挡 join。 */
    void bind(String agentId, String userId);

    /**
     * @return 登记的 JWT {@code sub}；无记录或读失败时 empty
     */
    Optional<String> findOwner(String agentId);

    /** leave 成功后删除归属。 */
    void unbind(String agentId);
}
