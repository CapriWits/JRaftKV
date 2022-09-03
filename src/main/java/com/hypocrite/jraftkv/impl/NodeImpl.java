package com.hypocrite.jraftkv.impl;

import com.hypocrite.client.ClientKVRequest;
import com.hypocrite.client.ClientKVResponse;
import com.hypocrite.jraftkv.*;
import com.hypocrite.jraftkv.common.RaftNodeStatus;
import com.hypocrite.jraftkv.common.changes.Companion;
import com.hypocrite.jraftkv.common.changes.CompanionSet;
import com.hypocrite.jraftkv.common.changes.MemberShipChangesRes;
import com.hypocrite.jraftkv.common.concurrent.JRaftThreadPool;
import com.hypocrite.jraftkv.common.consensus.AppendEntriesParam;
import com.hypocrite.jraftkv.common.consensus.AppendEntriesRes;
import com.hypocrite.jraftkv.common.consensus.RequestVoteParam;
import com.hypocrite.jraftkv.common.consensus.RequestVoteRes;
import com.hypocrite.jraftkv.common.entity.Log;
import com.hypocrite.jraftkv.common.entity.LogEntry;
import com.hypocrite.jraftkv.common.entity.ReplicationFailModel;
import com.hypocrite.jraftkv.config.NodeConfig;
import com.hypocrite.jraftkv.rpc.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Default Node character is Follower
 *
 * @Author: Hypocrite30
 * @Date: 2022/8/17 11:24
 */
@Data
@Slf4j
public class NodeImpl implements Node, ClusterMembershipChanges {

    private volatile long electionTime = 15L * 1000;
    private volatile long prevElectionTime = 0;      // Previous election timestamp
    private volatile long prevHeartBeatTime = 0;     // Previous heartbeat timestamp
    public static final long HEARTBEAT_TICK = 5L * 1000;
    private volatile int identity = RaftNodeStatus.FOLLOWER; // Default character is Follower
    private volatile boolean isRunning = false;
    private CompanionSet companionSet;
    private HeartBeatRunnable heartBeatRunnable = new HeartBeatRunnable();
    private ElectionRunnable electionRunnable = new ElectionRunnable();
    private ReplicationFailConsumer replicationFailConsumer = new ReplicationFailConsumer();
    private LinkedBlockingQueue<ReplicationFailModel> replicationFailQueue = new LinkedBlockingQueue<>(1024);
    private NodeConfig nodeConfig;
    private RpcServer rpcServer;    // initialization when calling #setConfig
    private RpcClient rpcClient;
    private StateMachine stateMachine;
    private RaftConsensus consensus;
    private ClusterMembershipChanges membershipChanges;
    private final State state;

    @Data
    public class State {
        /* Persistent state on all servers(Updated on stable storage before responding to RPCs) */
        private volatile long currentTerm = 0;  // Term start from 0, increases monotonically
        private volatile String votedFor;       // candidateId that received vote in current term(or null if none)
        private LogModule logModule;            // Log entries; each entry contains command for state machine, and term when entry was received by leader(first index is 1)

        /* Volatile state on all servers */
        private volatile long commitIndex;      // index of the highest log entry known to be committed
        private volatile long lastApplied = 0;  // index of the highest log entry applied to state machine(initialized to 0, increases monotonically)

        /* Volatile state on leaders(Reinitialized after election) */
        private Map<Companion, Long> nextIndex;  // for each server, index of the next log entry to send to that server(initialized to leader last log index + 1)
        private Map<Companion, Long> matchIndex; // for each server, index of the highest log entry known to be replicated on server(initialized to 0, increases monotonically)
    }

    private NodeImpl() {
        rpcClient = new RpcClientImpl();
        state = new State();
    }

    private enum Singleton {
        INSTANCE;

        private NodeImpl node;

        Singleton() {
            node = new NodeImpl();
        }

        public NodeImpl getInstance() {
            return node;
        }
    }

    public static NodeImpl getInstance() {
        return Singleton.INSTANCE.getInstance();
    }

    @Override
    public void initialize() {
        isRunning = true;
        rpcServer.initialize();
        rpcClient.initialize();
        consensus = new RaftConsensusImpl(this);
        membershipChanges = new ClusterMembershipChangesImpl(this);
        JRaftThreadPool.scheduledWithFixedDelay(heartBeatRunnable, 500);
        JRaftThreadPool.scheduledAtFixedRate(electionRunnable, 6000, 500);
        JRaftThreadPool.execute(replicationFailConsumer);
        LogEntry logEntry = state.logModule.getLastLogEntry();
        if (logEntry != null) {
            state.setCurrentTerm(logEntry.getTerm());
        }
        log.info("Node has initialized successfully, self id: [{}]", companionSet.getSelf().getAddress());
    }

    @Override
    public void destroy() {
        rpcServer.destroy();
        stateMachine.destroy();
        rpcClient.destroy();
        isRunning = false;
        log.info("Node: [{}] has shutdown", companionSet.getSelf().getAddress());
    }

    @Override
    public void setConfig(NodeConfig config) {
        nodeConfig = config;
        stateMachine = config.getStateMachineType().getStateMachine();
        state.logModule = LogModuleImpl.getInstance();
        companionSet = CompanionSet.getInstance();
        for (String address : config.getCompanionAddresses()) {
            Companion companion = new Companion(address);
            companionSet.addCompanion(companion);
            if (("localhost:" + config.getSelfPort()).equals(address)) {
                companionSet.setSelf(companion);
            }
        }
        rpcServer = new RpcServerImpl(config.selfPort, this);
    }

    @Override
    public RequestVoteRes handleRequestVote(RequestVoteParam param) {
        log.info("Handle RequestVote, parameter: [{}]", param);
        return consensus.requestVote(param);
    }

    @Override
    public AppendEntriesRes handleAppendEntries(AppendEntriesParam param) {
        if (param.getLogEntries() != null) {
            log.info("Handle AppendEntries, leader: [{}], LogEntries: [{}]", param.getLeaderId(), param.getLogEntries());
        }
        return consensus.appendEntries(param);
    }

    /**
     * redirect client request to leader
     *
     * @param request Client request
     * @return ClientKVResponse
     */
    @Override
    public ClientKVResponse redirect(ClientKVRequest request) {
        RpcRequest rpcRequest = RpcRequest.builder()
                .requestBody(request)
                .url(companionSet.getLeader().getAddress())
                .requestType(RpcRequest.CLIENT_REQUEST).build();
        return rpcClient.sendRequest(rpcRequest);
    }

    @Override
    public MemberShipChangesRes addCompanion(Companion companion) {
        return membershipChanges.addCompanion(companion);
    }

    @Override
    public MemberShipChangesRes removeCompanion(Companion companion) {
        return membershipChanges.removeCompanion(companion);
    }

    /**
     * Only leader can handle client request, or others redirect request to leader.
     * Leader pre-commit to local storage and send AppendEntries RPC to other followers for log replication.
     * If there are more than half of successful responses from followers, leader will commit this log to state machine,
     * else reduce log index and retry to AppendEntries until all followers finish log replication eventually (although client has been replied).
     *
     * @param request Client request
     * @return ClientKVResponse
     */
    @Override
    public synchronized ClientKVResponse handleClientRequest(ClientKVRequest request) {
        log.info("Handle client request, request type: [{}], key: [{}], value: [{}]", request.getType(), request.getKey(), request.getValue());
        if (identity != RaftNodeStatus.LEADER) {
            log.info("Current node: [{}] is not leader, redirect to leader: [{}]", companionSet.getSelf().getAddress(), companionSet.getLeader());
            return redirect(request);
        }
        if (request.getType() == ClientKVRequest.GET) {
            LogEntry logEntry = stateMachine.getLogEntry(request.getKey());
            if (logEntry != null) {
                return new ClientKVResponse(logEntry.getLog());
            }
            return new ClientKVResponse(null);
        }
        LogEntry logEntry = LogEntry.builder()
                .log(Log.builder().key(request.getKey()).value(request.getValue()).build())
                .term(state.currentTerm).build();
        // pre-commit to local storage, but not commit
        state.logModule.write(logEntry);
        log.info("Write logModule successfully, LogEntry: [{}], Log index: [{}]", logEntry, logEntry.getIndex());

        final AtomicInteger success = new AtomicInteger(0);
        List<Future<Boolean>> futureList = new ArrayList<>();
        int cnt = 0;
        // LogEntry replication
        for (Companion companion : companionSet.getCompanionListExceptMySelf()) {
            cnt++;
            futureList.add(replication(companion, logEntry));
        }
        CountDownLatch latch = new CountDownLatch(futureList.size());
        List<Boolean> result = new CopyOnWriteArrayList<>();
        getAppendEntriesRPCResult(futureList, latch, result);
        try {
            latch.await(4000, MILLISECONDS);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
        for (Boolean b : result) {
            if (b) {
                success.incrementAndGet();
            }
        }
        /**
         * If there exists an N such that N > commitIndex, a majority
         * of matchIndex[i] ≥ N, and log[N].term == currentTerm:
         * set commitIndex = N (§5.3, §5.4).
         */
        List<Long> matchIndexList = new ArrayList<>(state.matchIndex.values());
        int median = 0;
        if (matchIndexList.size() >= 2) {
            Collections.sort(matchIndexList);
            median = matchIndexList.size() >>> 1;
        }
        Long N = matchIndexList.get(median);
        if (N > state.commitIndex) {
            LogEntry entry = state.logModule.read(N);
            if (entry != null && entry.getTerm() == state.currentTerm) {
                state.setCommitIndex(N);
            }
        }
        // more than half of the responses, commit this logEntry
        if (success.get() >= (cnt >>> 1)) {
            state.setCommitIndex(logEntry.getIndex());
            stateMachine.apply(logEntry);
            state.setLastApplied(state.commitIndex);
            log.info("Apply local state machine successfully, LogEntry: [{}]", logEntry);
            return ClientKVResponse.success();
        } else {
            state.logModule.removeFromStartIndex(logEntry.getIndex());
            log.info("Fail to apply local state machine, LogEntry: [{}]", logEntry);
            return ClientKVResponse.fail();
        }
    }

    public Future<Boolean> replication(Companion companion, LogEntry logEntry) {
        return JRaftThreadPool.submit(() -> {
            long start = System.currentTimeMillis(), end = start;
            // AppendEntries every 20s
            while (end - start < 20L * 1000) {
                AppendEntriesParam appendEntriesParam = AppendEntriesParam.builder()
                        .term(state.currentTerm)
                        .serverId(companion.getAddress())
                        .leaderId(companionSet.getSelf().getAddress())
                        .leaderCommit(state.commitIndex).build();
                Long nextIndex = state.nextIndex.get(companion);
                LinkedList<LogEntry> logEntries = new LinkedList<>();
                if (logEntry.getIndex() >= nextIndex) {
                    for (long i = nextIndex, len = logEntry.getIndex(); i <= len; i++) {
                        LogEntry entry = state.logModule.read(i);
                        if (entry != null) {
                            logEntries.add(entry);
                        }
                    }
                } else {
                    logEntries.add(logEntry);
                }
                LogEntry prevLog = getPrevLog(logEntries.getFirst());
                appendEntriesParam.setPrevLogTerm(prevLog.getTerm());
                appendEntriesParam.setPrevLogIndex(prevLog.getIndex());
                appendEntriesParam.setLogEntries(logEntries.toArray(new LogEntry[0]));
                RpcRequest rpcRequest = RpcRequest.builder()
                        .requestType(RpcRequest.APPEND_ENTRIES)
                        .requestBody(appendEntriesParam)
                        .url(companion.getAddress())
                        .build();
                // send AppendEntries RPC request
                AppendEntriesRes appendEntriesRes = rpcClient.sendRequest(rpcRequest);
                if (appendEntriesRes == null) {
                    return false;
                }
                if (appendEntriesRes.isSuccess()) {
                    state.nextIndex.put(companion, logEntry.getIndex() + 1);
                    state.matchIndex.put(companion, logEntry.getIndex());
                    log.info("Append to follower log entry successfully, follower: [{}], entry: [{}]", companion.getAddress(), appendEntriesParam.getLogEntries());
                    return true;
                } else { // fail
                    if (appendEntriesRes.getTerm() > state.currentTerm) {
                        state.currentTerm = appendEntriesRes.getTerm();
                        identity = RaftNodeStatus.FOLLOWER;
                        log.info("Follower's: [{}] term: [{}] is greater than my term: [{}], current node transform to Follower",
                                companion.getAddress(), appendEntriesRes.getTerm(), state.currentTerm);
                        return false;
                    } else {
                        // this case means log index or term is not matched, try to reduce nextIndex and retry log replication
                        if (nextIndex == 0) {
                            nextIndex = 1L;
                        }
                        state.nextIndex.put(companion, nextIndex - 1);
                        log.info("Follower's: [{}] nextIndex: [{}] is not matched. Reduce nextIndex to retry AppendEntries RPC",
                                companion.getAddress(), nextIndex);
                    }
                }
                end = System.currentTimeMillis();
            }
            return false;   // time out
        });
    }

    private LogEntry getPrevLog(LogEntry logEntry) {
        LogEntry entry = state.logModule.read(logEntry.getIndex() - 1);
        if (entry == null) {
            log.info("Previous log is null, LogEntry: [{}]", logEntry);
            entry = LogEntry.builder().index(0L).term(0).log(null).build();
        }
        return entry;
    }

    private void getAppendEntriesRPCResult(List<Future<Boolean>> futureList, CountDownLatch latch, List<Boolean> result) {
        for (Future<Boolean> future : futureList) {
            JRaftThreadPool.execute(() -> {
                try {
                    result.add(future.get(3000, MILLISECONDS));
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    result.add(false);
                } finally {
                    latch.countDown();
                }
            });
        }
    }

    class ReplicationFailConsumer implements Runnable {
        @Override
        public void run() {
            while (isRunning) {
                try {
                    ReplicationFailModel model = replicationFailQueue.poll(1000, MILLISECONDS);
                    if (model == null) {
                        continue;
                    }
                    if (identity != RaftNodeStatus.LEADER) {
                        replicationFailQueue.clear();
                        continue;
                    }
                    log.info("Replication fail worker consume a task, task LogEntry: [{}]", model.getLogEntry());
                    long offerTime = model.getOfferTime();
                    if (System.currentTimeMillis() - offerTime > 60L * 1000) {
                        log.info("Replication time out fail");
                    }
                    Callable callable = model.getCallable();
                    Future<Boolean> future = JRaftThreadPool.submit(callable);
                    Boolean b = future.get(3000, MILLISECONDS);
                    if (b) {
                        applyToStateMachine(model);
                    }
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }

        private void applyToStateMachine(ReplicationFailModel model) {
            String successKey = stateMachine.getString(model.getSuccessKey());
            stateMachine.setString(model.getSuccessKey(), String.valueOf(Integer.parseInt(successKey) + 1));
            String countKey = stateMachine.getString(model.getCountKey());
            // success more than half
            if (Integer.parseInt(successKey) >= (Integer.parseInt(countKey) >>> 1)) {
                stateMachine.apply(model.getLogEntry());
                stateMachine.delString(model.getCountKey(), model.getSuccessKey());
            }
        }
    }

    /**
     * Election task running when node starts
     * - increase currentTerm;
     * - Vote myself;
     * - Reset timeout timer;
     * <p>
     * If node receive more than half of votes, transform to Leader.
     * If node receive AppendEntries RPC, transform to Follower.
     * If election timeout, try again.
     */
    class ElectionRunnable implements Runnable {
        @Override
        public void run() {
            if (identity == RaftNodeStatus.LEADER) {
                return;
            }
            long curTime = System.currentTimeMillis();
            // random election time staggering
            electionTime = electionTime + ThreadLocalRandom.current().nextInt(50);
            if (curTime - prevElectionTime < electionTime) {
                return;
            }
            identity = RaftNodeStatus.CANDIDATE;
            log.info("Node: [{}] transform to Candidate, current term: [{}], LastEntry: [{}]",
                    companionSet.getSelf().getAddress(), state.currentTerm, state.logModule.getLastLogEntry());
            prevElectionTime = System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(200) + 150;
            state.currentTerm++;
            state.setVotedFor(companionSet.getSelf().getAddress());
            List<Companion> companions = companionSet.getCompanionListExceptMySelf();
            List<Future<RequestVoteRes>> futureList = new ArrayList<>();
            log.info("Other companions: [{}]", companions);
            // send RequestVote RPC
            for (Companion companion : companions) {
                futureList.add(JRaftThreadPool.submit(() -> {
                    long lastTerm = 0L;
                    LogEntry lastLogEntry = state.logModule.getLastLogEntry();
                    if (lastLogEntry != null) {
                        lastTerm = lastLogEntry.getTerm();
                    }
                    RequestVoteParam param = RequestVoteParam.builder()
                            .term(state.currentTerm)
                            .candidateId(companionSet.getSelf().getAddress())
                            .lastLogIndex(state.logModule.getLastIndex() == null ? 0L : state.logModule.getLastIndex())
                            .lastLogTerm(lastTerm).build();
                    RpcRequest rpcRequest = RpcRequest.builder()
                            .requestType(RpcRequest.REQUEST_VOTE)
                            .requestBody(param)
                            .url(companion.getAddress()).build();
                    try {
                        return rpcClient.<RequestVoteRes>sendRequest(rpcRequest);
                    } catch (Exception e) {
                        log.error("RequestVote RPC error, request url: [{}]", rpcRequest.getUrl());
                        return null;
                    }
                }));
            }
            AtomicInteger success = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(futureList.size());
            for (Future future : futureList) {
                JRaftThreadPool.submit(() -> {
                    try {
                        RequestVoteRes response = (RequestVoteRes) future.get(3000, MILLISECONDS);
                        if (response == null) {
                            return -1;
                        }
                        boolean isVoteGranted = response.isVoteGranted();
                        if (isVoteGranted) {
                            success.incrementAndGet();
                        } else {
                            // update current term
                            long resTerm = response.getTerm();
                            if (resTerm >= state.currentTerm) {
                                state.setCurrentTerm(resTerm);
                            }
                        }
                        return 0;
                    } catch (Exception e) {
                        log.error("Election error", e);
                        return -1;
                    } finally {
                        latch.countDown();
                    }
                });
            }
            try {
                latch.await(3500, MILLISECONDS);
            } catch (InterruptedException e) {
                log.error("InterruptedException from Election task", e);
            }
            int affect = success.get();
            log.info("Node: [{}] become leader, affected count: [{}], current node identify: [{}]",
                    companionSet.getSelf().getAddress(), affect, RaftNodeStatus.Enum.value(identity));
            if (identity == RaftNodeStatus.FOLLOWER) {
                return;
            }
            // receive more than half of votes and transform to Leader
            if (affect >= (companions.size() >>> 1)) {
                identity = RaftNodeStatus.LEADER;
                companionSet.setLeader(companionSet.getSelf());
                state.setVotedFor("");
                leaderInitialization();
                log.info("Node: [{}] transform to Leader", companionSet.getSelf().getAddress());
            } else {
                // elect again
                state.setVotedFor("");
            }
        }

        /**
         * All companion's nextIndex + 1
         * If Follower and Leader do not match in next RPC, it will fail.
         * Then Leader reduce nextIndex until the final consensus.
         */
        private void leaderInitialization() {
            state.nextIndex = new ConcurrentHashMap<>();
            state.matchIndex = new ConcurrentHashMap<>();
            for (Companion companion : companionSet.getCompanionListExceptMySelf()) {
                state.nextIndex.put(companion, state.logModule.getLastIndex() + 1);
                state.matchIndex.put(companion, 0L);
            }
        }
    }

    class HeartBeatRunnable implements Runnable {
        @Override
        public void run() {
            if (identity != RaftNodeStatus.LEADER) {
                return;
            }
            long curTime = System.currentTimeMillis();
            if (curTime - prevHeartBeatTime < HEARTBEAT_TICK) {
                return;
            }
            log.info("--- Heart Beat start ---");
            List<Companion> companions = companionSet.getCompanionListExceptMySelf();
            for (Companion companion : companions) {
                log.info("HeartBeat, Companion: [{}], nextIndex: [{}]", companion.getAddress(), state.nextIndex.get(companion));
            }
            prevHeartBeatTime = System.currentTimeMillis();
            for (Companion companion : companions) {
                AppendEntriesParam param = AppendEntriesParam.builder()
                        .logEntries(null)   // empty package
                        .leaderId(companionSet.getSelf().getAddress())
                        .serverId(companion.getAddress())
                        .term(state.currentTerm).build();
                RpcRequest rpcRequest = RpcRequest.builder()
                        .requestType(RpcRequest.APPEND_ENTRIES)
                        .requestBody(param)
                        .url(companion.getAddress()).build();
                JRaftThreadPool.execute(() -> {
                    try {
                        AppendEntriesRes response = rpcClient.sendRequest(rpcRequest);
                        long term = response.getTerm();
                        if (term > state.currentTerm) {
                            state.setCurrentTerm(term);
                            state.setVotedFor("");
                            identity = RaftNodeStatus.FOLLOWER;
                            log.info("Current node transform to Follower, HeartBeat term: [{}], currentTerm: [{}]", term, state.currentTerm);
                        }
                    } catch (Exception e) {
                        log.error("HeartBeat error, request url: [{}]", rpcRequest.getUrl());
                    }
                }, false);
            }
        }
    }
}
