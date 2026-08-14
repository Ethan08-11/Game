package cc.shturl.wa.demo.mq.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * MQ 事件类型枚举
 */
@Getter
@AllArgsConstructor
public enum MqEventType {

    /** 示例数据创建 */
    EXAMPLE_CREATED("EXAMPLE_CREATED", "示例数据创建");

    /** 事件编码 */
    private final String code;

    /** 事件描述 */
    private final String description;
}
