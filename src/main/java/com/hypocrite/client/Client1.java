package com.hypocrite.client;

import com.hypocrite.jraftkv.common.entity.LogEntry;
import com.hypocrite.jraftkv.util.ThreadUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Hypocrite30
 * @Date: 2022/8/5 0:56
 */
@Slf4j
public class Client1 {
    public static void main(String[] args) {
        ClientRPCModule rpcModule = new ClientRPCModule();
        for (int i = 3; i >= 0; i++) {
            try {
                String key = "key1:" + i, value = "value1:" + i;
                String rpcResponse = rpcModule.put(key, value);
                log.info("key: [{}], value: [{}], rpc put response: [{}]", key, value, rpcResponse);

                ThreadUtils.sleep(1000);

                LogEntry logEntry = rpcModule.get(key);
                log.info("key: [{}], value: [{}], rpc get response LogEntry: [{}]", key, value, logEntry);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                i--;
            }
            ThreadUtils.sleep(5000);
        }
    }
}
