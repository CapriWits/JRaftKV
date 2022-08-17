package com.hypocrite.jraftkv.rpc;

import com.alipay.remoting.BizContext;
import com.hypocrite.client.ClientKVRequest;
import com.hypocrite.jraftkv.ClusterMembershipChanges;
import com.hypocrite.jraftkv.common.changes.Companion;
import com.hypocrite.jraftkv.common.consensus.AppendEntriesParam;
import com.hypocrite.jraftkv.common.consensus.RequestVoteParam;
import com.hypocrite.jraftkv.impl.NodeImpl;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Hypocrite30
 * @Date: 2022/8/17 14:58
 */
@Slf4j
public class RpcServerImpl implements RpcServer {

    private final NodeImpl node;

    private final com.alipay.remoting.rpc.RpcServer rpcServer;

    public RpcServerImpl(int port, NodeImpl node) {
        this.node = node;
        rpcServer = new com.alipay.remoting.rpc.RpcServer(port, false, false);
        rpcServer.registerUserProcessor(new RaftUserProcessor<RpcRequest>() {
            @Override
            public Object handleRequest(BizContext bizContext, RpcRequest rpcRequest) {
                return handleRpcRequest(rpcRequest);
            }
        });
    }

    @Override
    public RpcResponse handleRpcRequest(RpcRequest rpcRequest) {
        RpcResponse<Object> response = null;
        int type = rpcRequest.getRequestType();
        // Branch Prediction, AppendEntries RPC is the most case
        if (type == RpcRequest.APPEND_ENTRIES) {
            response = new RpcResponse<>(node.handleAppendEntries((AppendEntriesParam) rpcRequest.getRequestBody()));
        } else {
            switch (type) {
                case RpcRequest.REQUEST_VOTE:
                    response = new RpcResponse<>(node.handleRequestVote(((RequestVoteParam) rpcRequest.getRequestBody())));
                    break;
                case RpcRequest.CLIENT_REQUEST:
                    response = new RpcResponse<>(node.handleClientRequest((ClientKVRequest) rpcRequest.getRequestBody()));
                    break;
                case RpcRequest.MEMBERSHIP_ADD:
                    response = new RpcResponse<>(((ClusterMembershipChanges) node).addCompanion(((Companion) rpcRequest.getRequestBody())));
                    break;
                case RpcRequest.MEMBERSHIP_REMOVE:
                    response = new RpcResponse<>(((ClusterMembershipChanges) node).removeCompanion(((Companion) rpcRequest.getRequestBody())));
            }
        }
        return response;
    }

    @Override
    public void initialize() {
        rpcServer.startup();
        log.info("RPC Server has started up");
    }

    @Override
    public void destroy() {
        rpcServer.shutdown();
        log.info("RPC Server has been shutdown");
    }
}
