package com.hypocrite.client;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author: Hypocrite30
 * @Date: 2022/8/17 0:39
 */
@Data
@Builder
public class ClientKVRequest implements Serializable {

    public static final int PUT = 0;
    public static final int GET = 1;

    private String key;

    private String value;

    private int type;

    public enum Type {
        PUT(0), GET(1);

        int code;

        Type(int code) {
            this.code = code;
        }

        public static Type getValue(int code) {
            for (Type type : values()) {
                if (type.code == code) {
                    return type;
                }
            }
            return null;
        }
    }
}
