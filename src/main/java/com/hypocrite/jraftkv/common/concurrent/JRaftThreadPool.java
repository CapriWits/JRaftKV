package com.hypocrite.jraftkv.common.concurrent;

import java.util.concurrent.*;

/**
 * @Author: Hypocrite30
 * @Date: 2022/8/19 13:06
 */
public class JRaftThreadPool {

    private static final int CORE_SIZE = Runtime.getRuntime().availableProcessors();
    private static final int MAX_POOL_SIZE = CORE_SIZE * 2;
    private static final int queueSize = 1024;
    private static final long KEEP_TIME = 1000 * 60;
    private static TimeUnit keepTimeUnit = TimeUnit.MILLISECONDS;
    private static ScheduledExecutorService scheduledExecutorService = getScheduledExecutorService();
    private static ThreadPoolExecutor executor = getThreadPoolExecutor();

    public static void scheduledAtFixedRate(Runnable r, long initDelay, long period) {
        scheduledExecutorService.scheduleAtFixedRate(r, initDelay, period, TimeUnit.MILLISECONDS);
    }

    public static void scheduledWithFixedDelay(Runnable r, long delay) {
        scheduledExecutorService.scheduleWithFixedDelay(r, 0, delay, TimeUnit.MILLISECONDS);
    }

    public static <T> Future<T> submit(Callable r) {
        return executor.submit(r);
    }

    public static void execute(Runnable r) {
        executor.execute(r);
    }

    public static void execute(Runnable r, boolean sync) {
        if (sync) {
            r.run();
        } else {
            executor.execute(r);
        }
    }

    private static ThreadPoolExecutor getThreadPoolExecutor() {
        return new JRaftThreadPoolExecutor(CORE_SIZE, MAX_POOL_SIZE, KEEP_TIME, keepTimeUnit, new LinkedBlockingQueue<>(queueSize), new NameThreadFactory());
    }

    private static ScheduledExecutorService getScheduledExecutorService() {
        return new ScheduledThreadPoolExecutor(CORE_SIZE, new NameThreadFactory());
    }

    static class NameThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new JRaftThread("JRaft thread", r);
            t.setDaemon(true);
            t.setPriority(5);
            return t;
        }
    }
}
