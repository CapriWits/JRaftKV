# JRaftKV

JRaftKV is a KV-storage based on [Raft](https://raft.github.io/) Consensus Algorithm(CP) in Java.

Project implementation design and details refer to the [paper](https://raft.github.io/raft.pdf).

## Achievement

- [x] Leader election
- [x] Log replication
- [x] Membership changes

## Architecture

![JRaftKV architecture](.\doc\JraftKV achietecture.svg)

- **StateMachine**: Log persistence module.
- **LogModule**: Focus on maintaining log index.

- **Consensus**: Including `RequestVotes` & `AppendEntries` .

- A `NodeImpl` contains StateMachine, LogModule, Consensus modules. Default indentity is 'Follower'. Nodes communicate with each other through RPC module.

## Verification

### Leader election

1. Start JRaftKV clusters with five nodes(`JRaftNodeApplication`). VM options: `-DserverPort=8775 -DserverPort=8776 -DserverPort=8777 -DserverPort=8778 -DserverPort=8779` .
2. Watch terminal operation log to check the Leader election process.
3. Candidate send `RequestVotes` and become Leader if it receives more than half of votes.

4.  Leader send `AppendEntries` empty package for heartbeat.

### Log replication

1. Start JRaftKV clusters with five nodes(`JRaftNodeApplication`). VM options: `-DserverPort=8775 -DserverPort=8776 -DserverPort=8777 -DserverPort=8778 -DserverPort=8779` .
2. Start Client and send request to leader or redirect to leader to handle it.
3. Leader will replicate log to all of Followers eventually.

## Reference

- [https://raft.github.io/](https://raft.github.io/)
- [https://github.com/stateIs0/lu-raft-kv](https://github.com/stateIs0/lu-raft-kv)

- https://liyafu.com/2021-11-25-how-raft-works/
