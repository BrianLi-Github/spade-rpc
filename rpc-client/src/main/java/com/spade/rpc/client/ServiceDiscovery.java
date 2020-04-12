package com.spade.rpc.client;

import com.spade.rpc.common.Constant;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created with IntelliJ IDEA.
 *
 * @Auther: Brian
 * @Date: 2020/04/12/9:52
 * @Description: 服务发现类，业务客户端通过spring容器构造ServiceDiscovery，RPC动态代理类RpcProxy即可在发送消息时找到可用的服务器信息
 */
public class ServiceDiscovery {

    private final static Logger LOGGER = LoggerFactory.getLogger(ServiceDiscovery.class);

    /**
     * zookeeper集群地址，可通过spring创建构造函数时初始化
     * {host1}:{port1},{host2}:{port2},{host3}:{port3}......
     */
    private String registryAddress;

    private volatile List<String> serverList;

    private ZooKeeper zooKeeper;
    private CountDownLatch latch = new CountDownLatch(1);

    public ServiceDiscovery(String registryAddress) {
        this.registryAddress = registryAddress;
        //业务类的Spring容器一启动即可连接zookeeper
        try {
            connectZookeeper();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void connectZookeeper() throws IOException, InterruptedException, KeeperException {
        final Long connectTime = System.currentTimeMillis();
        LOGGER.info("Initializing zookeeper client...");

        zooKeeper = new ZooKeeper(registryAddress, Constant.ZK_SESSION_TIMEOUT, new Watcher() {
            @Override
            public void process(WatchedEvent watchedEvent) {
                try {
                    Event.EventType eventType = watchedEvent.getType();
                    switch (eventType) {
                        case None:
                            if (Event.KeeperState.SyncConnected.equals(watchedEvent.getState())) {
                                //zookeeper连接成功，释放等待锁
                                LOGGER.info("Zookeeper connect successfully, took {} ms.", System.currentTimeMillis() - connectTime);
                                latch.countDown();
                            }
                            break;
                        case NodeChildrenChanged:
                            //监听到服务器上下线， 更新服务器列表信息
                            LOGGER.info("Zookeeper node children changed.");
                            changeServerList();
                            break;
                        default:
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        latch.await();
        changeServerList();
    }

    /**
     * 监听到服务器上下线，更新服务器列表信息
     *
     * @throws KeeperException
     * @throws InterruptedException
     */
    public void changeServerList() throws KeeperException, InterruptedException {
        List<String> serverInfos = new ArrayList<>();
        List<String> children = zooKeeper.getChildren(Constant.ZK_REGISTRY_PATH, true);
        children.parallelStream().forEach(childName -> {
            try {
                byte[] addressData = zooKeeper.getData(Constant.ZK_REGISTRY_PATH + "/" + childName, false, null);
                serverInfos.add(new String(addressData));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        LOGGER.info("Latest server list is {} ", serverInfos);
        this.serverList = serverInfos;
    }

    public String discover() {
        String serverAddress = "";
        if (serverList != null && !serverList.isEmpty()) {
            if (serverList.size() == 1) {
                serverAddress = serverList.get(0);
            } else {
                serverAddress = serverList.get(ThreadLocalRandom.current().nextInt(serverList.size()));
            }
        }
        System.out.println("Discovered server address [" + serverAddress + "] from server list " + serverList);
        return serverAddress;
    }

    public static void main(String[] args) {
        String REGISTRY_ADDRESS = "192.168.71.101:2181,192.168.71.102:2181,192.168.71.103:2181";
        ServiceDiscovery serviceDiscovery = new ServiceDiscovery(REGISTRY_ADDRESS);
        String serverAddress = serviceDiscovery.discover();
        System.out.println(serverAddress);
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
