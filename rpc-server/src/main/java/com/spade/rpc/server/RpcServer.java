package com.spade.rpc.server;

import com.spade.rpc.common.RpcRequest;
import com.spade.rpc.common.RpcResponse;
import com.spade.rpc.server.annotation.RpcService;
import com.spade.rpc.common.utils.RpcDecoder;
import com.spade.rpc.common.utils.RpcEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.HashMap;
import java.util.Map;

/**
 * spring加载时，获取自定义标签的service，存入bean map中，以便在netty线程中使用
 * 业务服务器将RpcServer配置到Spring容器内，即可执行setApplicationContext和afterPropertiesSet这两个方法
 */
public class RpcServer implements ApplicationContextAware, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcServer.class);

    /**
     * 业务服务器地址， {host}:{port}
     */
    private String serverAddress;
    private ServiceRegistry serviceRegistry;

    public RpcServer(String serverAddress, ServiceRegistry serviceRegistry) {
        this.serverAddress = serverAddress;
        this.serviceRegistry = serviceRegistry;
    }

//    public RpcServer(String serverAddress) {
//        this.serverAddress = serverAddress;
//    }

    private Map<String, Object> handlerBeanMap = new HashMap<String, Object>();

    /**
     * spring加载时会调用setApplicationContext方法，在这个方式中初始化所有自定义的RpcService
     *
     * @param applicationContext
     * @throws BeansException
     */
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, Object> beanMap = applicationContext.getBeansWithAnnotation(RpcService.class);
        if (beanMap != null) {
            beanMap.forEach((key, obj) -> {
                String serviceName = obj.getClass().getAnnotation(RpcService.class).value().getName();
                handlerBeanMap.put(serviceName, obj);
            });
        }
    }

    /**
     * spring容器加载完后执行
     * --> 启动netty，在此启动netty服务，绑定handle流水线：
     * 1、接收请求数据进行反序列化得到request对象
     * 2、根据request中的参数，让RpcHandler从handlerMap中找到对应的业务impl，调用指定方法，获取返回结果
     * 3、将业务调用结果封装到response并序列化后发往客户端
     * <p>
     * --> 将server注册到zookeeper集群中
     *
     * @throws Exception
     */
    public void afterPropertiesSet() throws Exception {
        EventLoopGroup primaryLoopGroup = new NioEventLoopGroup();
        EventLoopGroup workerLoopGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap server = new ServerBootstrap();
            server.group(primaryLoopGroup, workerLoopGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel channel) throws Exception {
                            //step1: Inbound，接受客户端请求参数，解码
                            channel.pipeline().addLast(new RpcDecoder(RpcRequest.class));
                            //step3: OutBound，将方法执行的返回结果通过RpcEncoder编码，响应客户端
                            channel.pipeline().addLast(new RpcEncoder(RpcResponse.class));
                            //step2: Inbound，根据decoder反序列出来的请求信息，反射成相应的方法执行，返回结果
                            channel.pipeline().addLast(new RpcServerHandler(handlerBeanMap));

                        }
                    }).option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            if (serverAddress == null || "".equals(serverAddress.trim())) {
                throw new Exception("Server address can not be null.");
            }
            //得到服务器地址信息
            String[] array = serverAddress.split(":");
            String host = array[0];
            int port = Integer.parseInt(array[1]);
            ChannelFuture channelFuture = server.bind(host, port).sync();
            System.out.println("服务器上线：" + channelFuture.channel().localAddress());

            //将服务服务器信息注册到zookeeper
            if (serviceRegistry != null) {
                boolean successfully = serviceRegistry.registry(serverAddress);
                if (!successfully) {
                   LOGGER.warn("Server address [{}] failed to registry to zookeeper.", serverAddress);
                }
            }
            channelFuture.channel().closeFuture().sync();
        } finally {
            primaryLoopGroup.shutdownGracefully().sync();
            workerLoopGroup.shutdownGracefully().sync();
        }
    }
}
