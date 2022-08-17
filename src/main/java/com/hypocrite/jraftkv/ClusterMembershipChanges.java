package com.hypocrite.jraftkv;

import com.hypocrite.jraftkv.common.changes.Companion;
import com.hypocrite.jraftkv.common.changes.MemberShipChangesRes;

/**
 * @Author: Hypocrite30
 * @Date: 2022/8/17 12:37
 */
public interface ClusterMembershipChanges {

    MemberShipChangesRes addCompanion(Companion companion);

    MemberShipChangesRes removeCompanion(Companion companion);

}
