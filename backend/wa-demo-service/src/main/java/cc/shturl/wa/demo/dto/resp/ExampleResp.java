package cc.shturl.wa.demo.dto.resp;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 示例响应 DTO
 */
@Data
public class ExampleResp {

    /** 主键 ID */
    private Long id;

    /** 示例名称 */
    private String name;

    /** 示例描述 */
    private String description;

    /** 创建时间 */
    private LocalDateTime createTime;
}
