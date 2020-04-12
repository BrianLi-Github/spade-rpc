package com.spade.rpc.client;

import com.spade.rpc.common.RpcRequest;
import com.spade.rpc.common.RpcResponse;
import com.spade.rpc.common.utils.RpcDecoder;
import com.spade.rpc.common.utils.RpcEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import io.netty.channel.socket.SocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

/**
 * Created with IntelliJ IDEA.
 *
 * @Auther: Brian
 * @Date: 2020/04/11/21:38
 * @Description: RPC客户端
 */
public class RpcClient extends SimpleChannelInboundHandler<RpcResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcClient.class);

    private String host;
    private int port;

    private RpcResponse response;
    //    private Object lock = new Object();
    private CountDownLatch countDownLatch = new CountDownLatch(1);

    public RpcClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * 向服务器发送请求
     *
     * @param request
     * @return
     * @throws Exception
     */
    public RpcResponse send(RpcRequest request) throws Exception {
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        try {

            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(eventLoopGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) throws Exception {
                            // 向pipeline中添加编码、解码、业务处理的handler
                            //step1: OutBound，将请求信息进行编码
                            channel.pipeline().addLast(new RpcEncoder(RpcRequest.class));
                            //step2: InBound，收到服务器的响应信息，将响应信息进行解码
                            channel.pipeline().addLast(new RpcDecoder(RpcResponse.class));
                            //step3: InBound，响应信息解码后进行业务处理（返回结果给调用的业务）
                            channel.pipeline().addLast(RpcClient.this);
                        }
                    }).option(ChannelOption.SO_KEEPALIVE, true);

            //连接服务器
            ChannelFuture future = bootstrap.connect(host, port).sync();
            //将request对象写入outboundHandler处理后发出（即RpcEncoder编码器）
            future.channel().writeAndFlush(request).sync();
            // 用线程等待的方式决定是否关闭连接
            // 其意义是：先在此阻塞，等待获取到服务端的返回后，被唤醒，从而关闭网络连接
            countDownLatch.await();
//        synchronized (lock) {
//            lock.wait();
//        }
            if (response != null) {
                future.channel().closeFuture().sync();
            }
            return response;
        } finally {
            eventLoopGroup.shutdownGracefully();
        }
    }

    /**
     * 读取到服务器的响应信息
     *
     * @param ctx
     * @param response
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcResponse response) throws Exception {
        this.response = response;
        countDownLatch.countDown();
//        synchronized (lock) {
//            lock.notifyAll();
//        }
    }


    /**
     * 异常处理
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        LOGGER.error("client caught exception", cause);
        ctx.close();
    }
}
