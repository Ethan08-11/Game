package cc.shturl.wa.demo.dto.resp;

public record TaskClaimResp(Long userTaskId, Long taskId, Integer progressValue, Integer targetValue,
                            Integer status, String message) {
}
