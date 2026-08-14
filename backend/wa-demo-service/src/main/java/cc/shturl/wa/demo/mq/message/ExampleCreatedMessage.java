package cc.shturl.wa.demo.mq.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 示例数据创建事件消息体
 * <p>
 * 由生产者发送到 RabbitMQ，消费者接收后执行异步处理
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExampleCreatedMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 事件类型标识 */
    private String eventType;

    /** 示例数据 ID */
    private Long exampleId;

    /** 示例名称 */
    private String name;

    /** 示例描述 */
    private String description;

    /** 事件发生时间 */
    private LocalDateTime occurredAt;
}
