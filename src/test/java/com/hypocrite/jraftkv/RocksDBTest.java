package com.hypocrite.jraftkv;

import com.alibaba.fastjson.JSON;
import lombok.Data;
import org.junit.Before;
import org.junit.Test;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.io.File;

/**
 * For RocksDB Java client testing
 *
 * @Author: Hypocrite30
 * @Date: 2022/8/9 1:03
 */
public class RocksDBTest {
    private final String dbDir = "./JRaft-RocksDB/" + System.getProperty("PORT");
    private final String stateMachineDir = dbDir + "/test";

    public RocksDB rocksDB;

    static {
        RocksDB.loadLibrary();  // a static method that loads the RocksDB C++ library.
    }

    public byte[] lastIndexKey = "LAST_INDEX_KEY".getBytes();

    public RocksDBTest() {
        try {
            System.setProperty("PORT", "8078");
            File file = new File(stateMachineDir);
            if (!file.exists()) {
                file.mkdirs();
            }
            Options options = new Options();
            options.setCreateIfMissing(true);
            rocksDB = RocksDB.open(options, stateMachineDir);
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }


    public static RocksDBTest getInstance() {
        return RocksDBTestLazyHolder.INSTANCE;
    }

    private static class RocksDBTestLazyHolder {
        private static final RocksDBTest INSTANCE = new RocksDBTest();
    }

    RocksDBTest instance;

    @Before
    public void before() {
        instance = getInstance();
    }

    @Test
    public void test() {
        System.out.println(getLastIndex());
        System.out.println(get(getLastIndex()));

        write(new Cmd("testKey", "testVal"));

        System.out.println(getLastIndex());

        System.out.println(get(getLastIndex()));

        deleteOnStartIndex(getLastIndex());

        write(new Cmd("testKey", "testVal"));

        deleteOnStartIndex(1L);

        System.out.println(getLastIndex());
        System.out.println(get(getLastIndex()));
    }

    public synchronized void write(Cmd cmd) {
        try {
            cmd.setIndex(getLastIndex() + 1);
            rocksDB.put(cmd.getIndex().toString().getBytes(), JSON.toJSONBytes(cmd));
        } catch (RocksDBException e) {
            e.printStackTrace();
        } finally {
            updateLastIndex(cmd.getIndex());
        }
    }

    public synchronized void deleteOnStartIndex(Long index) {
        try {
            for (long i = index; i <= getLastIndex(); i++) {
                try {
                    rocksDB.delete((i + "").getBytes());
                } catch (RocksDBException e) {
                    e.printStackTrace();
                }
            }
        } finally {
            updateLastIndex(index - 1);
        }
    }

    public Cmd get(Long index) {
        try {
            if (index == null) {
                throw new IllegalArgumentException();
            }
            byte[] cmd = rocksDB.get(index.toString().getBytes());
            if (cmd != null) {
                return JSON.parseObject(rocksDB.get(index.toString().getBytes()), Cmd.class);
            }
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void updateLastIndex(Long index) {
        try {
            // overWrite
            rocksDB.put(this.lastIndexKey, index.toString().getBytes());
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }

    public Long getLastIndex() {
        byte[] lastIndex = new byte[0];
        try {
            lastIndex = rocksDB.get(this.lastIndexKey);
            if (lastIndex == null) {
                lastIndex = "0".getBytes();
            }
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
        return Long.valueOf(new String(lastIndex));
    }

    @Data
    static class Cmd {
        Long index;
        String key;
        String value;

        public Cmd() {  // for FastJson reflection or get something wrong if missing
        }

        public Cmd(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

}
