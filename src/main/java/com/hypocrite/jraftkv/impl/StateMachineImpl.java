package com.hypocrite.jraftkv.impl;

import com.alibaba.fastjson.JSON;
import com.hypocrite.jraftkv.StateMachine;
import com.hypocrite.jraftkv.common.entity.Log;
import com.hypocrite.jraftkv.common.entity.LogEntry;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.io.File;

/**
 * @Author: Hypocrite30
 * @Date: 2022/8/16 0:04
 */
@Slf4j
public class StateMachineImpl implements StateMachine {

    public String dbDir;

    public String stateMachineDir;

    public RocksDB rocksDB;

    private StateMachineImpl() {
        dbDir = "./JRaft-RocksDB/" + System.getProperty("serverPort");
        stateMachineDir = dbDir + "/stateMachine";
        RocksDB.loadLibrary();  // a static method that loads the RocksDB C++ library.
        File file = new File(stateMachineDir);
        if (!file.exists()) {
            if (file.mkdirs()) {
                log.info("Have made a new dir : " + stateMachineDir);
            }
        }
        Options options = new Options();
        options.setCreateIfMissing(true);
        try {
            rocksDB = RocksDB.open(options, stateMachineDir);
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        }
    }

    public static StateMachineImpl getInstance() {
        return DefaultStateMachineLazyHolder.INSTANCE;
    }

    private static class DefaultStateMachineLazyHolder {
        private static final StateMachineImpl INSTANCE = new StateMachineImpl();
    }

    @Override
    public LogEntry getLogEntry(String key) {
        try {
            byte[] result = rocksDB.get(key.getBytes());
            if (result == null) {
                return null;
            }
            return JSON.parseObject(result, LogEntry.class);
        } catch (RocksDBException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    @Override
    public String getString(String key) {
        try {
            byte[] bytes = rocksDB.get(key.getBytes());
            if (bytes != null) {
                return new String(bytes);
            }
        } catch (RocksDBException e) {
            log.error(e.getMessage());
        }
        return "";
    }

    @Override
    public void setString(String key, String value) {
        try {
            rocksDB.put(key.getBytes(), value.getBytes());
        } catch (RocksDBException e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public void delString(String... key) {
        try {
            for (String s : key) {
                rocksDB.delete(s.getBytes());
            }
        } catch (RocksDBException e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public synchronized void apply(LogEntry logEntry) {
        try {
            Log log = logEntry.getLog();
            if (log == null) {
                throw new IllegalArgumentException("Log can't be null, logEntry: " + logEntry);
            }
            String key = log.getKey();
            rocksDB.put(key.getBytes(), JSON.toJSONBytes(logEntry));
            this.log.info("StateMachine has applied LogEntry successfully, LogEntry: [{}]", logEntry);
        } catch (RocksDBException e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public void initialize() {
    }

    @Override
    public void destroy() {
        rocksDB.close();
        log.info("Close RocksDB successfully");
    }
}
