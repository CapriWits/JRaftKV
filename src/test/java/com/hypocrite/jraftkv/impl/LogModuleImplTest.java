package com.hypocrite.jraftkv.impl;

import com.hypocrite.jraftkv.common.entity.Log;
import com.hypocrite.jraftkv.common.entity.LogEntry;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * For LogModule testing
 *
 * @Author: Hypocrite30
 * @Date: 2022/8/15 12:55
 */
public class LogModuleImplTest {

    static LogModuleImpl logModule = LogModuleImpl.getInstance();

    static {
        System.setProperty("serverPort", "8779");
        logModule.dbDir = "./rocksDB-raft/" + System.getProperty("PORT");
        logModule.logsDir = logModule.dbDir + "/logModule";
    }

    @Before
    public void before() {
        System.setProperty("PORT", "8777");
    }

    @After
    public void tearDown() {
    }

    @Test
    public void write() {
        LogEntry entry = LogEntry.builder().
                term(1).
                log(Log.builder().key("hello").value("world").build()).
                build();
        logModule.write(entry);
        Assert.assertEquals(entry, logModule.read(entry.getIndex()));
    }

    @Test
    public void read() {
        System.out.println(logModule.getLastIndex());
    }

    @Test
    public void remove() {
        logModule.removeFromStartIndex(3L);
    }

    @Test
    public void getLast() {
    }

    @Test
    public void getLastIndex() {
    }

    @Test
    public void getDbDir() {
    }

    @Test
    public void getLogsDir() {
    }

    @Test
    public void setDbDir() {
    }
}