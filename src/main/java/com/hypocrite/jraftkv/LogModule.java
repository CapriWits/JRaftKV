package com.hypocrite.jraftkv;

import com.hypocrite.jraftkv.common.entity.LogEntry;

/**
 * @Author: Hypocrite30
 * @Date: 2022/8/9 0:21
 */
public interface LogModule extends Lifecycle {

    void write(LogEntry LogEntry);

    LogEntry read(Long index);

    LogEntry getLastLogEntry();

    Long getLastIndex();

    void removeFromStartIndex(Long startIndex);

}
