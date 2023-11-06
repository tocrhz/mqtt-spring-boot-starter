package com.github.tocrhz.mqtt.properties;

import com.github.tocrhz.mqtt.subscriber.MqttSubscriber;
import com.github.tocrhz.mqtt.subscriber.TopicPair;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.LinkedList;
import java.util.Set;

/**
 * 配置类.
 */
@SuppressWarnings("unused")
public abstract class MqttConfigAdapter {
    protected MqttProperties mqttProperties;

    public final void setProperties(MqttProperties mqttProperties) {
        this.mqttProperties = mqttProperties;
        MqttClientRegistry clientRegistry = new MqttClientRegistry(mqttProperties);
        beforeCreate(clientRegistry);
    }

    /**
     * 在处理注解内参数之前.
     * 程序启动后执行，只会执行一次
     */
    public void beforeResolveEmbeddedValue(LinkedList<MqttSubscriber> subscribers) {

    }

    /**
     * 在处理注解内参数之后.
     * 程序启动后执行，只会执行一次
     */
    public void afterResolveEmbeddedValue(LinkedList<MqttSubscriber> subscribers) {

    }

    /**
     * 在创建客户端之前, 增删改客户端配置.
     * 程序启动后执行，只会执行一次
     *
     * @param registry ClientRegistry 添加或修改配置
     */
    public void beforeCreate(MqttClientRegistry registry) {

    }


    /**
     * 创建客户端.
     *
     * @param clientId   客户端ID
     * @param serverURIs serverURIs
     * @return IMqttAsyncClient
     * @throws MqttException 创建客户端异常
     */
    public IMqttAsyncClient postCreate(String clientId, String[] serverURIs) throws MqttException {
        return new MqttAsyncClient(serverURIs[0], clientId, new MemoryPersistence());
    }

    /**
     * 在创建客户端后, 订阅主题前, 修改订阅的主题.
     *
     * @param clientId 客户端ID
     * @param options  MqttConnectOptions
     */
    public void beforeConnect(String clientId, MqttConnectOptions options) {

    }

    /**
     * 在创建客户端后, 订阅主题前, 修改订阅的主题.
     *
     * @param clientId   客户端ID
     * @param topicPairs 订阅主题
     */
    public void beforeSubscribe(String clientId, Set<TopicPair> topicPairs) {

    }
}
