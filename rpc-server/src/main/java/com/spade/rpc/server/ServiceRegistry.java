package com.spade.rpc.server;

import com.spade.rpc.common.Constant;
import org.apache.zookeeper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * Created with IntelliJ IDEA.
 *
 * @Auther: Brian
 * @Date: 2020/04/12/10:28
 * @Description: 服务注册类，业务服务端一启动，则通过spring容器构造ServiceRegistry，即可往zookeeper注册服务器信息，告诉客户端该服务器可用
 */
public class ServiceRegistry {
    private final static Logger LOGGER = LoggerFactory.getLogger(ServiceRegistry.class);

    /**
     * zookeeper集群地址，可通过spring创建构造函数时初始化
     * {host1}:{port1},{host2}:{port2},{host3}:{port3}......
     */
    private String registryAddress;

    private ZooKeeper zooKeeper;
    private CountDownLatch latch = new CountDownLatch(1);

    public ServiceRegistry(String registryAddress) {
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
                        default:
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        latch.await();
        checkRegistryPathExists();
    }

    /**
     * 检查父节点是否存在，如果不存在则创建父节点
     *
     * @throws KeeperException
     * @throws InterruptedException
     */
    private void checkRegistryPathExists() throws KeeperException, InterruptedException {
        if (null == zooKeeper.exists(Constant.ZK_REGISTRY_PATH, false)) {
            zooKeeper.create(Constant.ZK_REGISTRY_PATH, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }
    }

    public boolean registry(String serverAddress) {
        if (zooKeeper == null) {
            LOGGER.error("Zookeeper connect not ready.");
            return false;
        }
        if (serverAddress == null) {
            LOGGER.error("Server address can not be null.");
            return false;
        }
        if (!serverAddress.contains(":")) {
            LOGGER.error("{} is not a server address.", serverAddress);
            return false;
        }
        try {
            checkRegistryPathExists();
            zooKeeper.create(Constant.ZK_DATA_PATH, serverAddress.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        LOGGER.info("Server [{}] registry to zookeeper successfully.", serverAddress);
        return true;
    }


    public static void main(String[] args) {
        String REGISTRY_ADDRESS = "192.168.71.101:2181,192.168.71.102:2181,192.168.71.103:2181";
        ServiceRegistry serviceRegistry = new ServiceRegistry(REGISTRY_ADDRESS);
        boolean registry = serviceRegistry.registry("localhost:8080");
        System.out.println(registry);
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
