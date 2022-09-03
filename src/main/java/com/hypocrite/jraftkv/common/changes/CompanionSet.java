package com.hypocrite.jraftkv.common.changes;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: Hypocrite30
 * @Date: 2022/8/18 9:07
 */
@Data
public class CompanionSet implements Serializable {

    private List<Companion> companionList = new ArrayList<>();

    private volatile Companion leader;

    private volatile Companion self;

    private CompanionSet() {
    }

    public static CompanionSet getInstance() {
        return Singleton.INSTANCE.getInstance();
    }

    private enum Singleton {
        INSTANCE;
        private CompanionSet companionSet;

        Singleton() {
            companionSet = new CompanionSet();
        }

        public CompanionSet getInstance() {
            return companionSet;
        }
    }

    public void addCompanion(Companion companion) {
        companionList.add(companion);
    }

    public List<Companion> getCompanionListExceptMySelf() {
        List<Companion> res = new ArrayList<>(companionList);
        res.remove(self);
        return res;
    }
}
