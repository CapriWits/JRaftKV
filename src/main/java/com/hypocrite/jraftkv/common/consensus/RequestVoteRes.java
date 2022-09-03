package com.hypocrite.jraftkv.common.consensus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @Author: Hypocrite30
 * @Date: 2022/8/8 0:10
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RequestVoteRes implements Serializable {
    private long term;  // current term, for candidate to update itself
    private boolean voteGranted;    // true if candidate received vote

    public RequestVoteRes(boolean voteGranted) {
        this.voteGranted = voteGranted;
    }

    public static RequestVoteRes fail() {
        return new RequestVoteRes(false);
    }
}
