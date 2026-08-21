package cc.shturl.wa.demo.dto.resp;

import java.util.List;

public record MyTaskBoardResp(
        List<UserTaskResp> tasks,
        Integer remainingMoney,
        Integer claimableCount,
        Boolean firstWinIncomplete,
        Long resetInSeconds
) {
}
