package com.hypocrite.jraftkv.impl;

import com.hypocrite.jraftkv.common.entity.Log;
import com.hypocrite.jraftkv.common.entity.LogEntry;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;

/**
 * For StateMachine testing
 *
 * @Author: Hypocrite30
 * @Date: 2022/8/16 22:53
 */
@Slf4j
public class StateMachineImplTest {

    static StateMachineImpl stateMachine = StateMachineImpl.getInstance();

    static {
        System.setProperty("serverPort", "8777");
        stateMachine.dbDir = "./JRaft-RocksDB/" + System.getProperty("serverPort");
        stateMachine.stateMachineDir = stateMachine.dbDir + "/stateMachine";
    }

    @Before
    public void before() {
        stateMachine = StateMachineImpl.getInstance();
    }

    @Test
    public void apply() {
        LogEntry logEntry = LogEntry.builder()
                .term(1)
                .log(Log.builder().key("hello").value("value1").build())
                .build();
        stateMachine.apply(logEntry);
    }

    @Test
    public void applyRead() {
        log.info(stateMachine.getLogEntry("hello").toString());
    }

}
