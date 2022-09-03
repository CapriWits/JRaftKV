package com.hypocrite.client;

import com.google.common.collect.Lists;
import com.hypocrite.jraftkv.rpc.RpcClient;
import com.hypocrite.jraftkv.rpc.RpcClientImpl;
import com.hypocrite.jraftkv.rpc.RpcRequest;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @Author: Hypocrite30
 * @Date: 2022/8/23 15:58
 */
public class ClientRPCModule {

    private static List<String> nodeList = Lists.newArrayList("localhost:8777", "localhost:8778", "localhost:8779");
    private static final RpcClient rpcClient = new RpcClientImpl();
    private AtomicLong count = new AtomicLong(3);

    public ClientRPCModule() {
        rpcClient.initialize();
    }

    public ClientKVResponse get(String key) {
        ClientKVRequest clientKVRequest = ClientKVRequest.builder().key(key).type(ClientKVRequest.GET).build();
        int index = (int) (count.incrementAndGet() % nodeList.size());
        String address = nodeList.get(index);
        RpcRequest rpcRequest = RpcRequest.builder()
                .requestBody(clientKVRequest)
                .url(address)
                .requestType(RpcRequest.CLIENT_REQUEST).build();
        ClientKVResponse logEntry;
        try {
            logEntry = rpcClient.sendRequest(rpcRequest);
        } catch (Exception e) {
            rpcRequest.setUrl(nodeList.get((int) (count.incrementAndGet() % nodeList.size())));
            logEntry = rpcClient.sendRequest(rpcRequest);
        }
        return logEntry;
    }

    public ClientKVResponse put(String key, String value) {
        int index = (int) (count.incrementAndGet() % nodeList.size());
        String address = nodeList.get(index);
        ClientKVRequest clientKVRequest = ClientKVRequest.builder().key(key).value(value).type(ClientKVRequest.PUT).build();
        RpcRequest rpcRequest = RpcRequest.builder()
                .requestBody(clientKVRequest)
                .url(address)
                .requestType(RpcRequest.CLIENT_REQUEST).build();
        ClientKVResponse response;
        try {
            response = rpcClient.sendRequest(rpcRequest);
        } catch (Exception e) {
            rpcRequest.setUrl(nodeList.get((int) ((count.incrementAndGet()) % nodeList.size())));
            response = rpcClient.sendRequest(rpcRequest);
        }
        return response;
    }
}
