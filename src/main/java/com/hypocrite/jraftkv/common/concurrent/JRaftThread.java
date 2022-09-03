package com.hypocrite.jraftkv.common.concurrent;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Hypocrite30
 * @Date: 2022/8/18 23:59
 */
@Slf4j
public class JRaftThread extends Thread {

    public JRaftThread(String threadName, Runnable runnable) {
        super(runnable, threadName);
        setUncaughtExceptionHandler((thread, exception) -> {
            log.warn("Exception occurred from thread: [{}]", thread.getName(), exception);
        });
    }

}
