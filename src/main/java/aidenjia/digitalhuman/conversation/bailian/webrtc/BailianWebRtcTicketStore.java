package con.aidenjia.digitalhuman.conversation.bailian.webrtc;

import java.util.Optional;

public interface BailianWebRtcTicketStore {

    void save(String relayId, String userId, String model);

    Optional<BailianWebRtcTicket> find(String relayId);

    void delete(String relayId);

    /** Atomically validate the owner and remove a live ticket; invalid owners do not consume it. */
    Optional<BailianWebRtcTicket> consume(String relayId, String userId);
}
