package com.hypocrite.jraftkv.rpc;

import com.alipay.remoting.AsyncContext;
import com.alipay.remoting.BizContext;
import com.alipay.remoting.rpc.protocol.AbstractUserProcessor;
import com.hypocrite.jraftkv.exception.RaftNotSupportedException;

/**
 * @Author: Hypocrite30
 * @Date: 2022/8/17 15:34
 */
public abstract class RaftUserProcessor<T> extends AbstractUserProcessor<T> {

    @Override
    public void handleRequest(BizContext bizContext, AsyncContext asyncContext, T rpcRequest) {
        throw new RaftNotSupportedException("Raft Server can't support handleRequest(BizContext bizContext, AsyncContext asyncContext, T rpcRequest)");
    }

    @Override
    public String interest() {
        return RpcRequest.class.getName();
    }
}
