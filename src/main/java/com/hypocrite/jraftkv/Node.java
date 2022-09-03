package com.hypocrite.jraftkv;

import com.hypocrite.client.ClientKVResponse;
import com.hypocrite.client.ClientKVRequest;
import com.hypocrite.jraftkv.common.consensus.AppendEntriesParam;
import com.hypocrite.jraftkv.common.consensus.AppendEntriesRes;
import com.hypocrite.jraftkv.common.consensus.RequestVoteParam;
import com.hypocrite.jraftkv.common.consensus.RequestVoteRes;
import com.hypocrite.jraftkv.config.NodeConfig;

/**
 * @Author: Hypocrite30
 * @Date: 2022/8/16 23:42
 */
public interface Node extends Lifecycle {

    void setConfig(NodeConfig config);

    AppendEntriesRes handleAppendEntries(AppendEntriesParam param);

    RequestVoteRes handleRequestVote(RequestVoteParam param);

    ClientKVResponse handleClientRequest(ClientKVRequest request);

    /**
     * Forward message to Leader
     *
     * @param request Client request
     * @return ClientKV response
     */
    ClientKVResponse redirect(ClientKVRequest request);

}
