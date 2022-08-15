package com.hypocrite.jraftkv.common.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @Author: Hypocrite30
 * @Date: 2022/8/7 23:58
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogEntry implements Serializable, Comparable<LogEntry> {
    private Long index;
    private long term;
    private Log log;

    @Override
    public int compareTo(LogEntry o) {
        if (o == null) return -1;
        return (int) (this.getIndex() - o.getIndex());
    }
}
