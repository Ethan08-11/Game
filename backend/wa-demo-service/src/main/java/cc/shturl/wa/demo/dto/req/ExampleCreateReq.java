package cc.shturl.wa.demo.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 示例创建请求 DTO
 */
@Data
public class ExampleCreateReq {

    /** 示例名称 */
    @NotBlank(message = "名称不能为空")
    private String name;

    /** 示例描述 */
    private String description;
}
