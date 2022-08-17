package com.hypocrite.jraftkv.rpc;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author: Hypocrite30
 * @Date: 2022/8/17 12:11
 */
@Data
@Builder
public class RpcResponse<T> implements Serializable {

    private T result;

    public RpcResponse(T result) {
        this.result = result;
    }

}
