package cc.shturl.wa.api.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 远程调用响应 DTO 示例
 */
@Data
public class RemoteUserResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    private Long id;

    /** 用户名 */
    private String username;
}
