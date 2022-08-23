package com.hypocrite.client;

import com.hypocrite.jraftkv.common.entity.LogEntry;
import com.hypocrite.jraftkv.util.ThreadUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Hypocrite30
 * @Date: 2022/8/5 0:56
 */
@Slf4j
public class Client3 {
    public static void main(String[] args) {
        ClientRPCModule rpcModule = new ClientRPCModule();
        int i = 4;
        try {
            String key = "key3:" + i, value = "value3:" + i;
            String rpcResponse = rpcModule.put(key, value);
            log.info("key: [{}], value: [{}], rpc put response: [{}]", key, value, rpcResponse);
            ThreadUtils.sleep(1000);
            LogEntry logEntry = rpcModule.get(key);
            if (logEntry == null) {
                log.info("rpc get response is null, key: [{}]", key);
                System.exit(1);
                return;
            }
            log.info("rpc get response LogEntry: [{}], key: [{}]", logEntry, key);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        System.exit(1);
    }
}
