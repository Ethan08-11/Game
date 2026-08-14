package cc.shturl.wa.common.result;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一 API 响应封装
 *
 * @param <T> 响应数据类型
 */
@Data
public class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 响应码 */
    private int code;

    /** 响应消息 */
    private String message;

    /** 响应数据 */
    private T data;

    /** 时间戳 */
    private long timestamp;

    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 成功（无数据）
     */
    public static <T> Result<T> ok() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    /**
     * 成功（带数据）
     */
    public static <T> Result<T> ok(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    /**
     * 成功（自定义消息 + 数据）
     */
    public static <T> Result<T> ok(String message, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    /**
     * 失败（默认错误码）
     */
    public static <T> Result<T> fail(String message) {
        return new Result<>(ResultCode.FAIL.getCode(), message, null);
    }

    /**
     * 失败（自定义错误码）
     */
    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 失败（使用 ResultCode 枚举）
     */
    public static <T> Result<T> fail(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return ResultCode.SUCCESS.getCode() == this.code;
    }
}
