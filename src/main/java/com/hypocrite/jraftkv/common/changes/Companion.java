package com.hypocrite.jraftkv.common.changes;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Other peers Node
 *
 * @Author: Hypocrite30
 * @Date: 2022/8/17 14:30
 */
@Data
@Builder
@EqualsAndHashCode
public class Companion {

    private final String address;

    public Companion(String address) {
        this.address = address;
    }
}
