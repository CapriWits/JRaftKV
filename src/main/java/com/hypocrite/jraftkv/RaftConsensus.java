package com.hypocrite.jraftkv;

import com.hypocrite.jraftkv.common.consensus.AppendEntriesParam;
import com.hypocrite.jraftkv.common.consensus.AppendEntriesRes;
import com.hypocrite.jraftkv.common.consensus.RequestVoteParam;
import com.hypocrite.jraftkv.common.consensus.RequestVoteRes;

/**
 * Raft Consensus module
 *
 * @Author: Hypocrite30
 * @Date: 2022/8/7 23:37
 */
public interface RaftConsensus {
    AppendEntriesRes appendEntries(AppendEntriesParam appendParam);

    RequestVoteRes requestVote(RequestVoteParam RequestParam);
}
