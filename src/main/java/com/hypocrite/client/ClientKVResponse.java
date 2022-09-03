package com.hypocrite.client;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author: Hypocrite30
 * @Date: 2022/8/17 0:40
 */
@Data
@Builder
public class ClientKVResponse implements Serializable {

    private Object result;

    public ClientKVResponse(Object result) {
        this.result = result;
    }

    public static ClientKVResponse success() {
        return new ClientKVResponse("success");
    }

    public static ClientKVResponse fail() {
        return new ClientKVResponse("fail");
    }
}
