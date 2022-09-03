package com.hypocrite.jraftkv.common.consensus;

import lombok.*;

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
@AllArgsConstructor
@NoArgsConstructor
public class AppendEntriesRes implements Serializable {
    private long term;          // current term, for leader to update itself
    private boolean success;    // true if follower contains entry matching prevLogIndex & prevLogTerm

    public AppendEntriesRes(boolean success) {
        this.success = success;
    }

    public static AppendEntriesRes fail() {
        return new AppendEntriesRes(false);
    }
}
