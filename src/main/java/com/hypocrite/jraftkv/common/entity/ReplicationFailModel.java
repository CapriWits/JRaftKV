package com.hypocrite.jraftkv.common.entity;

import com.hypocrite.jraftkv.common.changes.Companion;
import lombok.Data;

import java.util.concurrent.Callable;

/**
 * @Author: Hypocrite30
 * @Date: 2022/8/20 0:12
 */
@Data
public class ReplicationFailModel {
    private String countKey;
    private String successKey;
    private Callable callable;
    private LogEntry logEntry;
    private Companion companion;
    private Long offerTime;
}
