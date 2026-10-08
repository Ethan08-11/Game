package cc.shturl.wa.demo.service;

public interface ClientNetworkService {
    void rememberClient(Long userId, String ip, String deviceId);

    String ipOf(Long userId);

    String deviceOf(Long userId);

    void requireDistinctNetwork(Long leftUserId, Long rightUserId);
}
