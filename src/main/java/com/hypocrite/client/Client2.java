package com.hypocrite.client;

import com.hypocrite.jraftkv.util.ThreadUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Hypocrite30
 * @Date: 2022/8/5 0:56
 */
@Slf4j
public class Client2 {
    public static void main(String[] args) {
        ClientRPCModule rpcModule = new ClientRPCModule();
        for (int i = 3; ; i++) {
            try {
                String key = "key2:" + i;
                ClientKVResponse logEntry = rpcModule.get(key);
                log.info("key: [{}], rpc response LogEntry: [{}]", key, logEntry);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            } finally {
                ThreadUtils.sleep(1000);
            }
        }
    }
}
