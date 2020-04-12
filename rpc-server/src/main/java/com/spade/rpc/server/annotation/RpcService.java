package com.spade.rpc.server.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Component  //将@RpcService 标注的bean放到spring中进行管理，业务的service类加上该注解即可
public @interface RpcService {
    Class<?> value();
}
