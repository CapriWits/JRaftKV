package com.hypocrite.jraftkv.exception;

/**
 * @Author: Hypocrite30
 * @Date: 2022/8/17 16:55
 */
public class RaftRemotingException extends RuntimeException {

    public RaftRemotingException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
