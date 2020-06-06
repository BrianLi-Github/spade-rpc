作者： Brian   email：1332966295@qq.com

本仓库的代码是鄙人编写的分布式RPC框架，如有建议，欢迎一起讨论

主要运用的技术和中间件有：
    -- zookeeper
    -- NIO（socket通信，netty框架实现）
    -- 动态代理（客户端调用的Service通过代理完成，在代理的逻辑里面获取zookeeper节点中可用的服务器地址）
    -- 反射机制（服务器获取socket channel中的ByteBuf，反射成对应的实现类进行方法调用）

框架调用流程：
1、项目的server端依赖rpc-server
2、在项目sever端的spring容器中配置服务注册类(ServiceRegistry)和RPCServer类
    --> com.spade.rpc.server.ServiceRegistry: 项目server端启动时构造服务注册类，并连接zookeeper，在ZK上初始化节点
    --> com.spade.rpc.server.RpcServer: socketServer端，
        项目server端启动setApplicationContext，会将RPC的service对象存入一个Map集合中，
        项目启动结束afterPropertiesSet，启动SocketServer端，监听Socket Client的通信，并将服务地址信息注册到zookeeper
3、项目的client端依赖rpc-client
4、在项目client端的spring容器中配置服务发现类(ServiceDiscovery)和RpcProxy类
    --> com.spade.rpc.client.ServiceDiscovery:项目client端启动，连接ZK并获取ZK上可用的服务器地址信息，存入list缓存中
    --> com.spade.rpc.client.RpcProxy:动态代理工具类，项目client端的业务类可以注入RpcProxy,
        通过RpcProxy.create方法创建代理，从list缓存中得到可用服务器地址信息，
        通过该地址信息创建Socket client，并向SocketServer发送信息
    

