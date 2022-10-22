package com.github.tocrhz.mqtt.autoconfigure;

import com.github.tocrhz.mqtt.properties.ConnectionProperties;
import com.github.tocrhz.mqtt.properties.MqttProperties;
import com.github.tocrhz.mqtt.subscriber.TopicPair;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.util.Assert;

import java.util.Set;

/**
 * 配置类.
 */
public abstract class MqttConfigurer {
    protected MqttProperties mqttProperties;


    public final void setProperties(MqttProperties mqttProperties) {
        this.mqttProperties = mqttProperties;
        ClientRegistry clientRegistry = new ClientRegistry();
        beforeCreate(clientRegistry);
    }

    /**
     * 在创建客户端之前, 增删改客户端配置.
     *
     * @param registry ClientRegistry 添加或修改配置
     */
    public void beforeCreate(ClientRegistry registry) {

    }


    /**
     * 创建客户端.
     *
     * @param clientId 客户端ID
     * @param options  MqttConnectOptions
     * @return IMqttAsyncClient
     * @throws MqttException 创建客户端异常
     */
    public IMqttAsyncClient postCreate(String clientId, MqttConnectOptions options) throws MqttException {
        return new MqttAsyncClient(options.getServerURIs()[0], clientId, new MemoryPersistence());
    }

    /**
     * 在创建客户端后, 订阅主题前, 修改订阅的主题.
     *
     * @param clientId   客户端ID
     * @param topicPairs 订阅主题
     */
    public void beforeSubscribe(String clientId, Set<TopicPair> topicPairs) {

    }

    public class ClientRegistry {

        /**
         * 禁用, 创建客户端之前.
         *
         * @return ClientRegistry
         */
        public ClientRegistry disable() {
            mqttProperties.setDisable(true);
            return this;
        }

        /**
         * 添加新的client, 添加到配置末尾.
         *
         * @param clientId 客户端ID
         * @param uri      连接地址
         * @return ClientRegistry
         */
        public ClientRegistry add(String clientId, String... uri) {
            Assert.notNull(clientId, "clientId can not be null.");
            ConnectionProperties properties = new ConnectionProperties();
            if (uri != null && uri.length > 0) {
                properties.setUri(uri);
            } else {
                properties.setUri(null);
            }
            properties.setClientId(clientId);
            mqttProperties.getClients().put(clientId, properties);
            resetClientId(clientId);
            return this;
        }

        /**
         * 添加新的client, 添加到配置末尾.
         *
         * @param clientId   客户端ID, 优先级比配置信息中的高
         * @param properties 配置信息
         * @return ClientRegistry
         */
        public ClientRegistry add(String clientId, ConnectionProperties properties) {
            Assert.notNull(clientId, "clientId can not be null.");
            Assert.notNull(properties, "properties can not be null.");
            properties.setClientId(clientId);
            mqttProperties.getClients().put(clientId, properties);
            resetClientId(clientId);
            return this;
        }

        /**
         * 删除指定的客户端ID配置.
         *
         * @param clientId 客户端ID
         * @return ClientRegistry
         */
        public ClientRegistry remove(String clientId) {
            Assert.notNull(clientId, "clientId can not be null.");
            mqttProperties.getClients().remove(clientId);
            resetClientId(clientId);
            return this;
        }

        /**
         * 清空所有客户端配置.
         *
         * @return ClientRegistry
         */
        public ClientRegistry clear() {
            mqttProperties.setClientId(null);
            mqttProperties.getClients().clear();
            return this;
        }

        /**
         * 设置默认.
         * @param properties 配置参数
         * @return ClientRegistry
         */
        public ClientRegistry setDefault(ConnectionProperties properties) {
            mqttProperties.setClientId(properties.getClientId());
            mqttProperties.setUsername(properties.getUsername());
            mqttProperties.setWill(properties.getWill());
            mqttProperties.setAutomaticReconnect(properties.getAutomaticReconnect());
            mqttProperties.setCleanSession(properties.getCleanSession());
            mqttProperties.setConnectionTimeout(properties.getConnectionTimeout());
            mqttProperties.setExecutorServiceTimeout(properties.getExecutorServiceTimeout());
            mqttProperties.setKeepAliveInterval(properties.getKeepAliveInterval());
            mqttProperties.setMaxReconnectDelay(properties.getMaxReconnectDelay());
            mqttProperties.setPassword(properties.getPassword());
            mqttProperties.setUri(properties.getUri());
            mqttProperties.setEnableSharedSubscription(properties.getEnableSharedSubscription());
            mqttProperties.setDefaultPublishQos(properties.getDefaultPublishQos());
            return this;
        }

        private void resetClientId(String clientId) {
            if (mqttProperties.getClientId() != null && clientId.equals(mqttProperties.getClientId())) {
                // 如果 clientId 和默认的 clientId 一样 则将默认的clientId清除掉
                // 原因：修改后的是通过clients集合保存的，如果与默认的clientId冲突, 则将默认的排除
                mqttProperties.setClientId(null);
            }
        }
    }
}
