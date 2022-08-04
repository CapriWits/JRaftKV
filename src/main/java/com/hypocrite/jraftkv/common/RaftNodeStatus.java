package com.hypocrite.jraftkv.common;

/**
 * @Author: Hypocrite30
 * @Date: 2022/8/5 0:57
 */
public interface RaftNodeStatus {

    int FOLLOWER = 0;
    int CANDIDATE = 1;
    int LEADER = 2;

    enum Enum {
        FOLLOWER(0), CANDIDATE(1), LEADER(2);

        Enum(int code) {
            this.code = code;
        }

        int code;

        public static Enum value(int i) {
            for (Enum value : Enum.values()) {
                if (value.code == i) {
                    return value;
                }
            }
            return null;
        }

    }

}
