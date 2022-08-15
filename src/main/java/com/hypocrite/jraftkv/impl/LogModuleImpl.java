package com.hypocrite.jraftkv.impl;

import com.alibaba.fastjson.JSON;
import com.hypocrite.jraftkv.LogModule;
import com.hypocrite.jraftkv.common.entity.LogEntry;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.io.File;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @Author: Hypocrite30
 * @Date: 2022/8/15 0:53
 */
@Slf4j
@Data
public class LogModuleImpl implements LogModule {

    public String dbDir;

    public String logsDir;

    private RocksDB rocksDB;

    public final static byte[] LAST_INDEX_KEY = "LAST_INDEX_KEY".getBytes();

    final ReentrantLock lock = new ReentrantLock();

    private LogModuleImpl() {
        if (dbDir == null) {
            dbDir = "./JRaft-RocksDB/" + System.getProperty("serverPort");
        }
        if (logsDir == null) {
            logsDir = dbDir + "/logModule";
        }
        RocksDB.loadLibrary();  // a static method that loads the RocksDB C++ library.
        Options options = new Options();
        options.setCreateIfMissing(true);
        // try to create DB files if not exist
        File file = new File(logsDir);
        if (!file.exists()) {
            if (file.mkdirs()) {
                log.info("Have made a new dir : " + logsDir);
            }
        }
        try {
            rocksDB = RocksDB.open(options, logsDir);
        } catch (RocksDBException e) {
            log.warn(e.getMessage());
        }
    }

    public static LogModuleImpl getInstance() {
        return LogModuleImplLazyHolder.INSTANCE;
    }

    private static class LogModuleImplLazyHolder {
        private static final LogModuleImpl INSTANCE = new LogModuleImpl();
    }

    @Override
    public LogEntry read(Long index) {
        try {
            byte[] result = rocksDB.get(serialize(index));
            if (result == null) {
                return null;
            }
            return JSON.parseObject(result, LogEntry.class);
        } catch (RocksDBException e) {
            log.warn(e.getMessage(), e);
        }
        return null;
    }

    /**
     * logEntry's index as key, which is incremental
     *
     * @param logEntry LogEntry to be stored
     */
    @Override
    public void write(LogEntry logEntry) {
        boolean isSucc = false;
        try {
            lock.tryLock(3000, MILLISECONDS);
            logEntry.setIndex(getLastIndex() + 1);
            rocksDB.put(logEntry.getIndex().toString().getBytes(), JSON.toJSONBytes(logEntry));
            isSucc = true;
            log.info("LogModule write rocksDB successfully, logEntry info: [{}]", logEntry);
        } catch (RocksDBException | InterruptedException e) {
            log.warn(e.getMessage());
        } finally {
            if (isSucc) {
                updateLastIndex(logEntry.getIndex());
            }
            lock.unlock();
        }
    }

    @Override
    public void removeFromStartIndex(Long startIndex) {
        boolean isSucc = false;
        int count = 0;
        try {
            lock.tryLock(3000, MILLISECONDS);
            for (long i = startIndex; i <= getLastIndex(); i++) {
                rocksDB.delete(String.valueOf(i).getBytes());
                count++;
            }
            isSucc = true;
            log.warn("RocksDB removeFromStartIndex successfully, count={} startIndex={}, lastIndex={}", count, startIndex, getLastIndex());
        } catch (InterruptedException | RocksDBException e) {
            log.warn(e.getMessage());
        } finally {
            if (isSucc) {
                updateLastIndex(getLastIndex() - count);
            }
            lock.unlock();
        }
    }

    @Override
    public LogEntry getLastLogEntry() {
        try {
            byte[] result = rocksDB.get(serialize(getLastIndex()));
            if (result == null) {
                return null;
            }
            return JSON.parseObject(result, LogEntry.class);
        } catch (RocksDBException e) {
            log.warn(e.getMessage());
        }
        return null;
    }

    @Override
    public Long getLastIndex() {
        byte[] lastIndex = "-1".getBytes();
        try {
            lastIndex = rocksDB.get(LAST_INDEX_KEY);
            if (lastIndex == null) {
                lastIndex = "-1".getBytes();
            }
        } catch (RocksDBException e) {
            log.warn(e.getMessage());
        }
        return Long.valueOf(new String(lastIndex));
    }

    /**
     * RocksDB use byte array as key
     *
     * @param key key
     * @return binary byte array
     */
    private byte[] serialize(Long key) {
        return key.toString().getBytes();
    }

    private void updateLastIndex(Long index) {
        try {
            rocksDB.put(LAST_INDEX_KEY, index.toString().getBytes());
        } catch (RocksDBException e) {
            log.warn(e.getMessage());
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
