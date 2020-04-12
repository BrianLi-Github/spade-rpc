package com.spade.rpc.server.smaple.service;

import com.spade.rpc.server.annotation.RpcService;
import com.spade.rpc.server.smaple.api.HelloService;
import com.spade.rpc.server.smaple.dto.Person;

import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 *
 * @Auther: Brian
 * @Date: 2020/04/12/13:46
 * @Description: 业务实现类
 */
@RpcService(HelloService.class)
public class HelloServiceImpl implements HelloService {
    @Override
    public String sayHello(String name) {
        return "Hello, " + name + ", Welcome!";
    }

    @Override
    public Person getPersonMessage(Person person) {
        Person resPerson = new Person();
        if (person != null) {
            resPerson = person;
            resPerson.setName(String.valueOf(person.getName()) + new Random().nextInt(100000));
        } else {
            resPerson.setName("new Name");
            resPerson.setMale(true);
        }
        return resPerson;
    }
}
