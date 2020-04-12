package com.spade.rpc.common;

import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 *
 * @Auther: Brian
 * @Date: 2020/04/11/20:37
 * @Description: RPC服务器响应信息
 */
@Data
public class RpcResponse {

    // 对应每一次请求的requestId，标识是当前这一次的请求响应
    private String requestId;
    private Throwable error;
    private Object result;

    public boolean isError() {
        return error != null;
    }
}
