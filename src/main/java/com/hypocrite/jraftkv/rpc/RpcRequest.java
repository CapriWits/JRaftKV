package com.hypocrite.jraftkv.rpc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @Author: Hypocrite30
 * @Date: 2022/8/17 12:12
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RpcRequest implements Serializable {

    public static final int REQUEST_VOTE = 0;
    public static final int APPEND_ENTRIES = 1;
    public static final int CLIENT_REQUEST = 2;
    public static final int MEMBERSHIP_ADD = 3;
    public static final int MEMBERSHIP_REMOVE = 4;

    private int requestType = -1;

    /**
     * @see com.hypocrite.jraftkv.common.consensus.AppendEntriesParam
     * @see com.hypocrite.jraftkv.common.consensus.RequestVoteParam
     * @see com.hypocrite.client.ClientKVRequest
     */
    private Object requestBody;

    private String url;

}
