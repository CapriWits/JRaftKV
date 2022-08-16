package com.hypocrite.jraftkv.common.consensus;

import com.hypocrite.jraftkv.common.entity.LogEntry;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

/**
 * AppendEntry parameter
 *
 * @Author: Hypocrite30
 * @Date: 2022/8/7 23:51
 */
@Data
@Builder
@ToString
public class AppendEntriesParam {
    private long term;          // leader's term
    private String leaderId;    // so follower can redirect clients
    private long prevLogIndex;  // index of log entry immediately preceding new ones
    private long prevLogTerm;   // term of prevLogIndex entry
    private long leaderCommit;  // index of log entry which the leader has submitted
    private LogEntry[] logEntries;  // log entries to store (empty for heartbeat; may send more than one for efficiency)
    private String serverId;    // respondent's id (self port)
}

