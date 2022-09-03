package com.hypocrite.jraftkv.common.changes;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: Hypocrite30
 * @Date: 2022/8/17 14:25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberShipChangesRes {

    public static final int FAIL = 0;
    public static final int SUCCESS = 1;

    private int status;

}
