package com.recruitos.common.exception;

/**
 * Business exception
 */
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;
    private final String msg;

    public BizException(String msg) {
        super(msg);
        this.code = 500;
        this.msg = msg;
    }

    public BizException(int code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
