package com.hypocrite.jraftkv.common;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * AppendEntry return result
 *
 * @Author: Hypocrite30
 * @Date: 2022/8/7 23:42
 */
@Data
@ToString
@Builder
public class AppendEntriesRes implements Serializable {
    private long term;          // current term, for leader to update itself
    private boolean success;    // true if follower contains entry matching prevLogIndex & prevLogTerm
}
