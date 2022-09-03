package com.hypocrite.jraftkv;

import com.hypocrite.jraftkv.config.NodeConfig;
import com.hypocrite.jraftkv.config.StateMachineType;
import com.hypocrite.jraftkv.impl.NodeImpl;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

/**
 * @Author: Hypocrite30
 * @Date: 2022/8/23 23:09
 */
@Slf4j
public class JRaftNodeApplication {

    public static void main(String[] args) {
        log.info("server port: [{}]", System.getProperty("serverPort"));
        start();
    }

    private static void start() {
        String property = System.getProperty("cluster.addr.list");
        String[] companions;
        if (StringUtil.isNullOrEmpty(property)) {
            companions = new String[]{"localhost:8775", "localhost:8776", "localhost:8777", "localhost:8778", "localhost:8779"};
        } else {
            companions = property.split(",");
        }
        NodeConfig config = new NodeConfig();
        // self
        config.setSelfPort(Integer.parseInt(System.getProperty("serverPort", "8779")));
        // others
        config.setCompanionAddresses(Arrays.asList(companions));
        config.setStateMachineType(StateMachineType.ROCKSDB);
        Node node = NodeImpl.getInstance();
        node.setConfig(config);
        node.initialize();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            synchronized (node) {
                node.notifyAll();
            }
        }));
        log.info("gracefully wait");
        synchronized (node) {
            try {
                node.wait();
            } catch (InterruptedException e) {
                log.info(e.getMessage(), e);
            }
        }
        log.info("gracefully stop");
        node.destroy();
    }
}
