package com.hypocrite.jraftkv.config;

import lombok.Data;

import java.util.List;

/**
 * @Author: Hypocrite30
 * @Date: 2022/8/16 23:48
 */
@Data
public class NodeConfig {

    public int selfPort;

    public List<String> companionAddresses;

    public StateMachineType stateMachineType;
}
