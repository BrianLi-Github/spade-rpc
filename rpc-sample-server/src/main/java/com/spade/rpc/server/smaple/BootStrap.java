package com.spade.rpc.server.smaple;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Created with IntelliJ IDEA.
 *
 * @Auther: Brian
 * @Date: 2020/04/12/13:53
 * @Description: 业务启动类，模拟web服务器（项目实际情况会丢到应用服务器去启动，或者通过jar -jar启动内置服务器）
 */
public class BootStrap {
    public static void main(String[] args) {
        new ClassPathXmlApplicationContext("classpath:spring.xml");
    }
}
