package com.spade.rpc.client;

import com.spade.rpc.common.RpcRequest;
import com.spade.rpc.common.RpcResponse;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 *
 * @Auther: Brian
 * @Date: 2020/04/11/22:11
 * @Description: RPC代理类，将客户端需要执行的方法通过代理类
 * 业务层通过spring创建该代理类即可注入到业务代码中
 */
public class RpcProxy {

    /**
     * 默认服务器地址，如果从zookeeper中动态获取可用地址失败，则使用该默认地址
     * {host}:{port}
     */
    private String defaultServerAddress;

    private ServiceDiscovery serviceDiscovery;

    public RpcProxy(String defaultServerAddress) {
        this.defaultServerAddress = defaultServerAddress;
    }

    public RpcProxy(String defaultServerAddress, ServiceDiscovery serviceDiscovery) {
        this.defaultServerAddress = defaultServerAddress;
        this.serviceDiscovery = serviceDiscovery;
    }

    public <T> T create(Class<?> interfaceClass) {
        return (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class<?>[]{interfaceClass}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                //把被代理类的属性封装成RpcRequest
                RpcRequest request = new RpcRequest();
                request.setRequestId(UUID.randomUUID().toString());
                //接口名称
                request.setClassName(method.getDeclaringClass().getName());
                //方法名称
                request.setMethodName(method.getName());
                //方法参数列表类型
                request.setParamTypes(method.getParameterTypes());
                //方法参数列表值
                request.setParams(args);

                String serverAddress = "";
                if (serviceDiscovery != null) {
                    serverAddress = serviceDiscovery.discover();
                }
                //如果从zookeeper中获取不到，则用默认的地址
                serverAddress = (serverAddress == null || "".equals(serverAddress.trim())) ? defaultServerAddress : serverAddress;
                if (serverAddress == null || "".equals(serverAddress.trim())) {
                    throw new Throwable("Unknown server host.");
                }
                //得到服务器地址信息
                String[] array = serverAddress.split(":");
                String host = array[0];
                int port = Integer.parseInt(array[1]);
                //创建RPC客户端，向服务器端发送请求
                RpcResponse response = new RpcClient(host, port).send(request);
                if (response.isError()) {
                    throw response.getError();
                }
                return response.getResult();
            }
        });
    }
}
