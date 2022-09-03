package com.hypocrite.jraftkv.impl;

import com.hypocrite.jraftkv.ClusterMembershipChanges;
import com.hypocrite.jraftkv.LogModule;
import com.hypocrite.jraftkv.common.RaftNodeStatus;
import com.hypocrite.jraftkv.common.changes.Companion;
import com.hypocrite.jraftkv.common.changes.MemberShipChangesRes;
import com.hypocrite.jraftkv.common.entity.LogEntry;
import com.hypocrite.jraftkv.rpc.RpcRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @Author: Hypocrite30
 * @Date: 2022/8/18 16:20
 */
@Slf4j
public class ClusterMembershipChangesImpl implements ClusterMembershipChanges {

    private final NodeImpl node;

    public ClusterMembershipChangesImpl(NodeImpl node) {
        this.node = node;
    }

    /**
     * Synchronized add companion
     *
     * @param companion to be added companion
     * @return ClusterMemberShipChangesResult
     */
    @Override
    public synchronized MemberShipChangesRes addCompanion(Companion companion) {
        List<Companion> companionListExceptMySelf = node.getCompanionSet().getCompanionListExceptMySelf();
        if (companion == null || companionListExceptMySelf.contains(companion)) {
            return new MemberShipChangesRes();
        }
        companionListExceptMySelf.add(companion);
        if (node.getIdentity() == RaftNodeStatus.LEADER) {
            NodeImpl.State state = node.getState();
            state.getNextIndex().put(companion, 0L);
            state.getMatchIndex().put(companion, 0L);
            LogModule logModule = state.getLogModule();
            for (long i = 0, len = logModule.getLastIndex(); i < len; i++) {
                LogEntry logEntry = logModule.read(i);
                if (logEntry != null) {
                    node.replication(companion, logEntry);
                }
            }
            for (Companion _ : companionListExceptMySelf) {
                RpcRequest rpcRequest = RpcRequest.builder().requestType(RpcRequest.MEMBERSHIP_ADD)
                        .url(companion.getAddress()).requestBody(companion).build();
                MemberShipChangesRes res = node.getRpcClient().sendRequest(rpcRequest);
                if (res != null && res.getStatus() == MemberShipChangesRes.SUCCESS) {
                    log.info("Replication config successful. Companion: [{}]", companion);
                } else {
                    log.info("Replication config fail. Companion: [{}]", companion);
                }
            }
        }
        return new MemberShipChangesRes();
    }

    /**
     * Synchronized remove companion
     *
     * @param companion tobe removed companion
     * @return ClusterMemberShipChangesResult
     */
    @Override
    public synchronized MemberShipChangesRes removeCompanion(Companion companion) {
        NodeImpl.State state = node.getState();
        node.getCompanionSet().getCompanionListExceptMySelf().remove(companion);
        state.getNextIndex().remove(companion);
        state.getMatchIndex().remove(companion);
        return new MemberShipChangesRes();
    }
}
