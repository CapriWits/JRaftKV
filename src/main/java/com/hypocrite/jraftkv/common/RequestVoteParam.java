package com.hypocrite.jraftkv.common;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author: Hypocrite30
 * @Date: 2022/8/8 0:10
 */
@Data
@Builder
public class RequestVoteParam implements Serializable {
    private long term;          // candidate's term
    private String candidateId; // candidate requesting vote
    private long lastLogIndex;  // index of candidate's last log entry
    private long lastLogTerm;   // term of candidate's last log entry
    private String serverId;    // respondent's id (self port)
}
