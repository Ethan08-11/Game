package cc.shturl.wa.demo.service;

import cc.shturl.wa.demo.dto.resp.UserPresenceResp;

import java.util.Collection;
import java.util.Map;

public interface UserPresenceService {
    UserPresenceResp getPresence(Long userId);

    Map<Long, UserPresenceResp> getPresences(Collection<Long> userIds);

    void requireInvitable(Long userId);

    void broadcastPresence(Long userId);
}
