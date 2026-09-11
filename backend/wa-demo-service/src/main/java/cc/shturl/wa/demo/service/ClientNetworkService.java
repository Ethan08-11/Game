package cc.shturl.wa.demo.service;

public interface ClientNetworkService {
    void rememberIp(Long userId, String ip);
    String ipOf(Long userId);
    void requireDistinctNetwork(Long leftUserId, Long rightUserId);
}
