package com.hypocrite.jraftkv.util;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Hypocrite30
 * @Date: 2022/8/23 19:24
 */
@Slf4j
public class ThreadUtils {

    public static void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }

}
