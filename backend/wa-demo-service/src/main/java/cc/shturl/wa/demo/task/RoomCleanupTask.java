package cc.shturl.wa.demo.task;

import cc.shturl.wa.demo.service.RoomPresenceCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomCleanupTask {
    private final RoomPresenceCleanupService cleanupService;

    @Scheduled(fixedDelayString = "${app.room.cleanup-interval-ms:30000}")
    public void cleanupExpiredRooms() {
        cleanupService.cleanupExpiredRoomMembers();
    }
}
