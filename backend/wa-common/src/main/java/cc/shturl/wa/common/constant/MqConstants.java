package cc.shturl.wa.common.constant;

/**
 * RabbitMQ 常量
 */
public final class MqConstants {

    private MqConstants() {
    }

    /** 示例交换机名称 */
    public static final String EXAMPLE_EXCHANGE = "wa.demo.exchange";

    /** 示例创建事件队列 */
    public static final String EXAMPLE_CREATED_QUEUE = "wa.demo.example.created.queue";

    /** 示例创建事件路由键 */
    public static final String EXAMPLE_CREATED_ROUTING_KEY = "wa.demo.example.created";

    /** 组队邀请事件交换机 */
    public static final String ROOM_EVENT_EXCHANGE = "wa.room.event.exchange";

    /** 邀请创建队列 */
    public static final String ROOM_INVITE_CREATED_QUEUE = "wa.room.invite.created.queue";

    /** 邀请接受队列 */
    public static final String ROOM_INVITE_ACCEPTED_QUEUE = "wa.room.invite.accepted.queue";

    /** 邀请拒绝队列 */
    public static final String ROOM_INVITE_REJECTED_QUEUE = "wa.room.invite.rejected.queue";

    /** 房间创建队列 */
    public static final String ROOM_CREATED_QUEUE = "wa.room.created.queue";

    /** 部门变更队列 */
    public static final String ROOM_MEMBER_DEPARTMENT_CHANGED_QUEUE = "wa.room.member.department.changed.queue";

    /** 准备状态变更队列 */
    public static final String ROOM_MEMBER_READY_QUEUE = "wa.room.member.ready.queue";

    /** 邀请创建路由键 */
    public static final String ROOM_INVITE_CREATED_ROUTING_KEY = "room.invite.created";

    /** 邀请接受路由键 */
    public static final String ROOM_INVITE_ACCEPTED_ROUTING_KEY = "room.invite.accepted";

    /** 邀请拒绝路由键 */
    public static final String ROOM_INVITE_REJECTED_ROUTING_KEY = "room.invite.rejected";

    /** 房间创建路由键 */
    public static final String ROOM_CREATED_ROUTING_KEY = "room.created";

    /** 部门变更路由键 */
    public static final String ROOM_MEMBER_DEPARTMENT_CHANGED_ROUTING_KEY = "room.member.department.changed";

    /** 准备状态变更路由键 */
    public static final String ROOM_MEMBER_READY_ROUTING_KEY = "room.member.ready";
}
