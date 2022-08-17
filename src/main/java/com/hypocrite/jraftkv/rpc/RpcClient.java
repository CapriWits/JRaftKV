package com.hypocrite.jraftkv.rpc;

import com.hypocrite.jraftkv.Lifecycle;

/**
 * @Author: Hypocrite30
 * @Date: 2022/8/17 12:09
 */
public interface RpcClient extends Lifecycle {

    <T> T sendRequest(RpcRequest rpcRequest);

    <T> T sendRequest(RpcRequest rpcRequest, int timeout);

}
