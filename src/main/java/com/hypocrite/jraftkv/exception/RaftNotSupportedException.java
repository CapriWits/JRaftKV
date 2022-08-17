package com.hypocrite.jraftkv.exception;

/**
 * @Author: Hypocrite30
 * @Date: 2022/8/17 15:39
 */
public class RaftNotSupportedException extends RuntimeException {

    public RaftNotSupportedException(String msg) {
        super(msg);
    }

}
