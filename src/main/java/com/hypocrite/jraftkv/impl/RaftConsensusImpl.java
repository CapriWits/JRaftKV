package com.hypocrite.jraftkv.impl;

import com.hypocrite.jraftkv.LogModule;
import com.hypocrite.jraftkv.RaftConsensus;
import com.hypocrite.jraftkv.common.RaftNodeStatus;
import com.hypocrite.jraftkv.common.changes.Companion;
import com.hypocrite.jraftkv.common.consensus.AppendEntriesParam;
import com.hypocrite.jraftkv.common.consensus.AppendEntriesRes;
import com.hypocrite.jraftkv.common.consensus.RequestVoteParam;
import com.hypocrite.jraftkv.common.consensus.RequestVoteRes;
import com.hypocrite.jraftkv.common.entity.LogEntry;
import io.netty.util.internal.StringUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author: Hypocrite30
 * @Date: 2022/8/17 17:48
 */
@Slf4j
@Data
public class RaftConsensusImpl implements RaftConsensus {

    private final NodeImpl node;
    private final NodeImpl.State state;

    private final ReentrantLock voteLock = new ReentrantLock();
    private final ReentrantLock appendLock = new ReentrantLock();

    public RaftConsensusImpl(NodeImpl node) {
        this.node = node;
        this.state = node.getState();
    }

    /**
     * Receiver implementation: <p>
     * 1. Reply false if term < currentTerm ($5.1) <p>
     * 2. If votedFor is null or candidateId, and candidate's log is at least as up-to-date as receiver's log, grant vote (§5.2, §5.4)
     *
     * @param param Request Vote parameter
     * @return Request Vote result
     */
    @Override
    public RequestVoteRes requestVote(RequestVoteParam param) {
        try {
            if (!voteLock.tryLock()) {
                log.error("VoteLock tryLock error");
                return RequestVoteRes.builder().term(state.getCurrentTerm()).voteGranted(false).build();
            }
            // 1. Reply false if term < currentTerm
            if (param.getTerm() < state.getCurrentTerm()) {
                log.info("RequestVote's term less than current's term");
                return RequestVoteRes.builder().term(state.getCurrentTerm()).voteGranted(false).build();
            }
            log.info("Current node: [{}], VoteFor: [{}], CandidateId: [{}], Current term: [{}], Companion term: [{}]",
                    node.getCompanionSet().getSelf(), state.getVotedFor(), param.getCandidateId(), state.getCurrentTerm(), param.getTerm());
            // 2. If votedFor is null or candidateId, and candidate's log is at least as up-to-date as receiver's log, grant vote
            if (StringUtil.isNullOrEmpty(state.getVotedFor()) || state.getVotedFor().equals(param.getCandidateId())) {
                LogModule logModule = state.getLogModule();
                if (logModule.getLastLogEntry() != null) {
                    // current term > requestVote term, reply false
                    if (logModule.getLastLogEntry().getTerm() > param.getLastLogTerm()) {
                        return RequestVoteRes.fail();
                    }
                    // current last log index > requestVote log index, reply false
                    if (logModule.getLastIndex() > param.getLastLogIndex()) {
                        return RequestVoteRes.fail();
                    }
                }
                node.setIdentity(RaftNodeStatus.FOLLOWER);
                node.getCompanionSet().setLeader(new Companion(param.getCandidateId()));
                state.setCurrentTerm(param.getTerm());
                state.setVotedFor(param.getServerId());
                return RequestVoteRes.builder().term(state.getCurrentTerm()).voteGranted(true).build();
            }
            return RequestVoteRes.builder().term(state.getCurrentTerm()).voteGranted(false).build();
        } finally {
            voteLock.unlock();
        }
    }

    /**
     * Receiver implementation:<p>
     * 1. Reply false if term < currentTerm (§5.1) <p>
     * 2. Reply false if log doesn’t contain an entry at prevLogIndex whose term matches prevLogTerm (§5.3) <p>
     * 3. If an existing entry conflicts with a new one (same index but different terms), delete the existing entry and all that follow it (§5.3) <p>
     * 4. Append any new entries not already in the log <p>
     * 5. If leaderCommit > commitIndex, set commitIndex = min(leaderCommit, index of last new entry) <p>
     *
     * @param param Append Entries RPC parameter
     * @return Append Entries result
     */
    @Override
    public AppendEntriesRes appendEntries(AppendEntriesParam param) {
        try {
            if (!appendLock.tryLock()) {
                log.error("AppendLock tryLock error");
                return AppendEntriesRes.fail();
            }
            // 1. Current term > AppendEntries term, reply false
            if (state.getCurrentTerm() > param.getTerm()) {
                return AppendEntriesRes.builder().term(state.getCurrentTerm()).success(false).build();
            }
            node.setPrevHeartBeatTime(System.currentTimeMillis());
            node.setPrevElectionTime(System.currentTimeMillis());
            node.getCompanionSet().setLeader(new Companion(param.getLeaderId()));
            // current term <= AppendEntries term, current node transform to Follower
            if (param.getTerm() >= state.getCurrentTerm()) {
                log.info("Current node: [{}] transform to Follower, current term: [{}], AppendEntries term: [{}], param serverId: [{}]",
                        node.getCompanionSet().getSelf(), state.getCurrentTerm(), param.getTerm(), param.getServerId());
                node.setIdentity(RaftNodeStatus.FOLLOWER);
            }
            state.setCurrentTerm(param.getTerm());  // update current term
            // HeartBeat AppendEntries
            if (param.getLogEntries() == null || param.getLogEntries().length == 0) {
                log.info("Receive HeartBeat package, leader: [{}], current term: [{}], leader's term: [{}]",
                        param.getLeaderId(), state.getCurrentTerm(), param.getTerm());
                return AppendEntriesRes.builder().term(state.getCurrentTerm()).success(true).build();
            }
            // LogEntry AppendEntries
            if (state.getLogModule().getLastIndex() != 0 && param.getPrevLogIndex() != 0) {
                LogEntry logEntry;
                if ((logEntry = state.getLogModule().read(param.getPrevLogIndex())) != null) {
                    // 2. Reply false if log doesn’t contain an entry at prevLogIndex whose term matches prevLogTerm
                    if (logEntry.getTerm() != param.getPrevLogTerm()) {
                        return AppendEntriesRes.builder().term(state.getCurrentTerm()).success(false).build();
                    }
                } else {
                    return AppendEntriesRes.builder().term(state.getCurrentTerm()).success(false).build();
                }
            }
            // 3. If an existing entry conflicts with a new one (same index but different terms), delete the existing entry and all that follow it
            LogEntry localLogEntry = state.getLogModule().read(param.getPrevLogIndex() + 1);
            if (localLogEntry != null && localLogEntry.getTerm() != param.getLogEntries()[0].getTerm()) {
                state.getLogModule().removeFromStartIndex(param.getPrevLogIndex() + 1);
            } else if (localLogEntry != null) {
                // log entry exist, no need to rewrite, reply true
                return AppendEntriesRes.builder().term(state.getCurrentTerm()).success(true).build();
            }
            // 4. Append any new entries not already in the log
            for (LogEntry logEntry : param.getLogEntries()) {
                state.getLogModule().write(logEntry);
                node.getStateMachine().apply(logEntry);
            }
            // 5. If leaderCommit > commitIndex, set commitIndex = min(leaderCommit, index of last new entry)
            if (param.getLeaderCommit() > state.getCommitIndex()) {
                int commitIndex = (int) Math.min(param.getLeaderCommit(), state.getLogModule().getLastIndex());
                state.setCommitIndex(commitIndex);
                state.setLastApplied(commitIndex);
            }
            // AppendEntries successful, set node as Follower
            node.setIdentity(RaftNodeStatus.FOLLOWER);
            return AppendEntriesRes.builder().term(state.getCurrentTerm()).success(true).build();
        } finally {
            appendLock.unlock();
        }
    }
}
