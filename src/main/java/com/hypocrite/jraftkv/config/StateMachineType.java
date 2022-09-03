package com.hypocrite.jraftkv.config;

import com.hypocrite.jraftkv.StateMachine;
import com.hypocrite.jraftkv.impl.StateMachineImpl;
import lombok.Getter;

/**
 * @Author: Hypocrite30
 * @Date: 2022/8/17 0:07
 */
@Getter
public enum StateMachineType {
    ROCKSDB("RocksDB", "RocksDB local storage", StateMachineImpl.getInstance());

    private String type;
    private String description;
    private StateMachine stateMachine;

    StateMachineType(String type, String description, StateMachine stateMachine) {
        this.type = type;
        this.description = description;
        this.stateMachine = stateMachine;
    }
}
