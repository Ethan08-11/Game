package cc.shturl.wa.common.constant;

/**
 * 公共常量类
 */
public final class CommonConstants {

    private CommonConstants() {
    }

    /** 逻辑删除 - 未删除 */
    public static final int NOT_DELETED = 0;

    /** 逻辑删除 - 已删除 */
    public static final int DELETED = 1;

    /** 默认分页页码 */
    public static final int DEFAULT_PAGE_NUM = 1;

    /** 默认分页大小 */
    public static final int DEFAULT_PAGE_SIZE = 10;

    /** 最大分页大小 */
    public static final int MAX_PAGE_SIZE = 100;
}
