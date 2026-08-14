package cc.shturl.wa.demo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("card_depts")
public class CardDepts extends BaseEntity {
    private String deptCode;
    private String deptName;
    private String deptCategory;
    private Integer sortNo;
    private Integer status;
}

