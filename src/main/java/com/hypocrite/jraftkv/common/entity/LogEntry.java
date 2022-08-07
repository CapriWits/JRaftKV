package com.hypocrite.jraftkv.common.entity;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author: Hypocrite30
 * @Date: 2022/8/7 23:58
 */
@Data
@Builder
public class LogEntry implements Serializable {
    private long index;
    private long term;
    private Log log;
}
