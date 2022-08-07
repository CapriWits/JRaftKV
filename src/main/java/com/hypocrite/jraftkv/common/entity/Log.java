package com.hypocrite.jraftkv.common.entity;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @Author: Hypocrite30
 * @Date: 2022/8/7 23:59
 */
@Data
@Builder
@ToString
public class Log implements Serializable {
    private String key;
    private String value;
}
