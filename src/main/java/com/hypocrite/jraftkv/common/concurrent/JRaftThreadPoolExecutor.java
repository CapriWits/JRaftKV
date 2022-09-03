package com.hypocrite.jraftkv.common.concurrent;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @Author: Hypocrite30
 * @Date: 2022/8/19 12:04
 */
@Slf4j
public class JRaftThreadPoolExecutor extends ThreadPoolExecutor {

    private static final ThreadLocal<Long> TIME_WATCHER = ThreadLocal.withInitial(System::currentTimeMillis);

    public JRaftThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                                   BlockingQueue<Runnable> workQueue, JRaftThreadPool.NameThreadFactory nameThreadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, nameThreadFactory);
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        Long startTime = TIME_WATCHER.get();
        log.debug("Thread pool beforeExecute, start time: [{}]", startTime);
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        log.debug("Thread pool afterExecute, cost time: [{}]", System.currentTimeMillis() - TIME_WATCHER.get());
    }

    @Override
    protected void terminated() {
        log.info("Thread pool, Active count: [{}], Queue size: [{}], Pool size: [{}]", getActiveCount(), getQueue().size(), getPoolSize());
    }
}
