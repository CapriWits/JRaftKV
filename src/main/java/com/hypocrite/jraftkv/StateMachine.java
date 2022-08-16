package com.hypocrite.jraftkv;

import com.hypocrite.jraftkv.common.entity.LogEntry;

/**
 * @Author: Hypocrite30
 * @Date: 2022/8/16 0:00
 */
public interface StateMachine extends Lifecycle {

    /**
     * Apply data to the state machine
     *
     * @param logEntry logEntry
     */
    void apply(LogEntry logEntry);

    LogEntry getLogEntry(String key);

    String getString(String key);

    void setString(String key, String value);

    void delString(String... key);

}
