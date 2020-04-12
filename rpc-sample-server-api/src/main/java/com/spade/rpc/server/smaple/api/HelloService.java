package com.spade.rpc.server.smaple.api;

import com.spade.rpc.server.smaple.dto.Person;

/**
 * Created with IntelliJ IDEA.
 *
 * @Auther: Brian
 * @Date: 2020/04/12/13:40
 * @Description: 业务接口
 */
public interface HelloService {

    String sayHello(String name);

    Person getPersonMessage(Person person);
}
