package com.hypocrite.jraftkv.rpc;

import com.alipay.remoting.exception.RemotingException;
import com.hypocrite.jraftkv.exception.RaftRemotingException;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * @Author: Hypocrite30
 * @Date: 2022/8/17 16:43
 */
@Slf4j
public class RpcClientImpl implements RpcClient {

    private final com.alipay.remoting.rpc.RpcClient rpcClient;

    public RpcClientImpl() {
        rpcClient = new com.alipay.remoting.rpc.RpcClient();
    }

    @Override
    public <T> T sendRequest(RpcRequest rpcRequest) {
        return sendRequest(rpcRequest, ((int) TimeUnit.SECONDS.toMillis(5)));
    }

    @Override
    public <T> T sendRequest(RpcRequest rpcRequest, int timeout) {
        RpcResponse<T> rpcResponse;
        try {
            rpcResponse = (RpcResponse<T>) rpcClient.invokeSync(rpcRequest.getUrl(), rpcRequest, timeout);
            return rpcResponse.getResult();
        } catch (RemotingException e) {
            throw new RaftRemotingException("RPC send request error", e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initialize() {
        rpcClient.startup();
        log.info("RPC Client has started up");
    }

    @Override
    public void destroy() {
        rpcClient.shutdown();
        log.info("RPC Client has been shutdown");
    }
}
