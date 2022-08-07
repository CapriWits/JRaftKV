package com.hypocrite.jraftkv;

import com.hypocrite.jraftkv.common.AppendEntriesParam;
import com.hypocrite.jraftkv.common.AppendEntriesRes;
import com.hypocrite.jraftkv.common.RequestVoteParam;
import com.hypocrite.jraftkv.common.RequestVoteRes;

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
