package com.hypocrite.jraftkv.common.entity;

import lombok.*;

import java.io.Serializable;

/**
 * @Author: Hypocrite30
 * @Date: 2022/8/7 23:59
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Log implements Serializable {
    private String key;
    private String value;
}
