package com.spade.rpc.client.smaple;

import com.spade.rpc.client.RpcProxy;
import com.spade.rpc.server.smaple.api.HelloService;
import com.spade.rpc.server.smaple.dto.Person;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:spring.xml")
public class HelloServiceTest {

	@Autowired
	private RpcProxy rpcProxy;

	@Test
	public void helloTest1() {
		// 调用代理的create方法，代理HelloService接口
		HelloService helloService = rpcProxy.create(HelloService.class);
		
		// 调用代理的方法，执行invoke
		String result = helloService.sayHello("Brian");
		System.out.println("服务端返回结果：");
		System.out.println(result);
	}

	@Test
	public void helloTest2() {
		Long time = System.currentTimeMillis();
		System.out.println("开始调用服务器的方法, time: " + time);
		HelloService helloService = rpcProxy.create(HelloService.class);
		Person person = helloService.getPersonMessage(new Person("Brian", 25, true));
		System.out.println("调用结束，耗时：" + (System.currentTimeMillis() - time) +" ms, 返回结果：");
		System.out.println(person);
	}
}
