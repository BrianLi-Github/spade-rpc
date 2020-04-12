package com.spade.rpc.server;

import com.spade.rpc.common.RpcRequest;
import com.spade.rpc.common.RpcResponse;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.EventExecutorGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @Auther: Brian
 * @Date: 2020/04/11/21:12
 * @Description:
 */
public class RpcServerHandler extends SimpleChannelInboundHandler<RpcRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcServerHandler.class);

    private final Map<String, Object> handlerBeanMap;

    public RpcServerHandler(Map<String, Object> handlerBeanMap) {
        this.handlerBeanMap = handlerBeanMap;
    }

    /**
     * 读取消息
     * 处理消息
     * 返回结果
     *
     * @param ctx
     * @param request
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequest request) throws Exception {
        RpcResponse response = new RpcResponse();
        response.setRequestId(request.getRequestId());
        try {
            response.setResult(handlerBusinesses(request));
        } catch (Throwable error) {
            response.setError(error);
        }
        //写入 outBoundHandler（即RpcEncoder）进行下一步处理（即编码）后发送到channel中给客户端
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        channelRead0(ctx, (RpcRequest) msg);
    }

    /**
     * 根据request来处理具体的业务调用
     * 通过反射的方式来完成
     *
     * @param request
     * @return
     */
    private Object handlerBusinesses(RpcRequest request) throws Throwable {
        String className = request.getClassName();
        Object serviceBean = handlerBeanMap.get(className);
        if (serviceBean == null) {
            LOGGER.error("Class [{}] not found in server.", className);
            throw new Throwable("Class: " + className + " not found.");
        }
        //拿到要调用的方法名、参数类型、参数值
        String methodName = request.getMethodName();
        Class<?>[] parameterTypes = request.getParamTypes();
        Object[] parameters = request.getParams();

        //拿到Service的类反射
        Class<?> clazz = Class.forName(className);

        //获取方法的反射
        Method method = clazz.getMethod(methodName, parameterTypes);
        //执行实现类对象的指定方法并返回结果
        return method.invoke(serviceBean, parameters);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("server caught exception", cause);
        ctx.close();
    }
}
