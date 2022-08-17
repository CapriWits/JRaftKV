package com.hypocrite.jraftkv.rpc;

import com.hypocrite.jraftkv.Lifecycle;

/**
 * @Author: Hypocrite30
 * @Date: 2022/8/17 12:10
 */
public interface RpcServer extends Lifecycle {

    RpcResponse handleRpcRequest(RpcRequest rpcRequest);

}
