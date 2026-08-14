package cc.shturl.wa.demo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 绀轰緥瀹炰綋绫伙紙璇锋浛鎹负瀹為檯涓氬姟瀹炰綋锛?
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_example")
public class ExampleEntity extends BaseEntity {

    /** 绀轰緥鍚嶇О */
    private String name;

    /** 绀轰緥鎻忚堪 */
    private String description;
}

