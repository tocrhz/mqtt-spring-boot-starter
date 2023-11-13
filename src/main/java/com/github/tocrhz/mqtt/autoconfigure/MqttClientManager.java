package com.github.tocrhz.mqtt.autoconfigure;

import com.github.tocrhz.mqtt.convert.MqttConversionService;
import com.github.tocrhz.mqtt.properties.MqttConfigAdapter;
import com.github.tocrhz.mqtt.properties.MqttConnectionProperties;
import com.github.tocrhz.mqtt.properties.MqttProperties;
import com.github.tocrhz.mqtt.publisher.SimpleMqttClient;
import com.github.tocrhz.mqtt.subscriber.MqttSubscriber;
import com.github.tocrhz.mqtt.subscriber.TopicPair;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 客户端连接管理一下
 */
@SuppressWarnings("unused")
public class MqttClientManager implements DisposableBean {
    private final static Logger log = LoggerFactory.getLogger(MqttClientManager.class);
    private final static LinkedHashMap<String, SimpleMqttClient> MQTT_CLIENT_MAP = new LinkedHashMap<>();
    private final MqttProperties properties;
    private final MqttConfigAdapter adapter;

    private String defaultClientId = null;

    public MqttClientManager(MqttProperties properties, MqttConfigAdapter adapter) {
        this.properties = properties;
        this.adapter = adapter;
        adapter.setProperties(properties);
    }

    public SimpleMqttClient clientNew(MqttConnectionProperties properties) {
        String clientId = properties.getClientId();

        Assert.hasText(clientId, "property clientId is required.");
        Assert.notEmpty(properties.getUri(), "property uri cannot be empty.");
        Assert.hasText(properties.getUri()[0], "property uri is required.");
        if (MQTT_CLIENT_MAP.containsKey(clientId)) {
            clientClose(clientId);
        }
        // 填充默认值
        this.properties.merge(properties);
        MqttConnectOptions options = this.properties.toOptions(properties);
        return clientNew(clientId, options, properties.getDefaultPublishQos());
    }

    void clientNew(String clientId, MqttConnectOptions options) {
        clientNew(clientId, options, null);
    }

    public SimpleMqttClient clientNew(String clientId, MqttConnectOptions options, Integer defaultPublishQos) {
        Assert.hasText(clientId, "clientId is required.");
        if (MQTT_CLIENT_MAP.containsKey(clientId)) {
            clientClose(clientId);
        }
        // 创建客户端
        IMqttAsyncClient client;
        try {
            client = adapter.postCreate(clientId, options.getServerURIs());
        } catch (MqttException e) {
            log.error("create mqtt client error: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
        // 创建topic
        boolean enableShared = this.properties.isEnableSharedSubscription(clientId);
        Set<TopicPair> topicPairs = mergeTopics(clientId, enableShared);
        int qos = defaultPublishQos != null ? defaultPublishQos : this.properties.getDefaultPublishQos(clientId);
        // 创建客户端对象
        SimpleMqttClient smc = new SimpleMqttClient(clientId, options, client, topicPairs, enableShared, qos, adapter);
        MQTT_CLIENT_MAP.put(clientId, smc);
        return smc;
    }

    public void clientClose(String clientId) {
        if (MQTT_CLIENT_MAP.containsKey(clientId)) {
            if (defaultClientId != null && defaultClientId.equals(clientId)) {
                String oldDefault = defaultClientId;
                String newDefault = null;
                for (SimpleMqttClient value : MQTT_CLIENT_MAP.values()) {
                    if (!value.id().equals(clientId)) {
                        newDefault = value.id();
                    }
                }
                // default 顺延到下一个， 如果有的话
                if (newDefault != null) {
                    defaultClientId = newDefault;
                    log.warn("default mqtt client '{}' closed, changed to '{}' ", oldDefault, newDefault);
                } else {
                    log.warn("default mqtt client '{}' closed, other client not exists. ", oldDefault);
                }
            }
            SimpleMqttClient client = MQTT_CLIENT_MAP.remove(clientId);
            client.close();
        }
    }

    public SimpleMqttClient clientGetOrDefault(String clientId) {
        if (StringUtils.hasText(clientId) && MQTT_CLIENT_MAP.containsKey(clientId)) {
            return MQTT_CLIENT_MAP.get(clientId);
        }
        return MQTT_CLIENT_MAP.get(defaultClientId);
    }

    public boolean setDefaultClientId(String clientId) {
        if (StringUtils.hasText(clientId) && MQTT_CLIENT_MAP.containsKey(clientId)) {
            defaultClientId = clientId;
            return true;
        }
        return false;
    }

    void afterInit() {

        // 初始化完成后，全部建立连接
        MQTT_CLIENT_MAP.forEach((id, client) -> {
            try {
                if (defaultClientId == null) {
                    defaultClientId = id;
                }
                client.connect();
            } catch (Exception e) {
                log.error("mqtt client '{}' close error: {}", id, e.getMessage(), e);
            }
        });
    }

    /**
     * 合并相似的主题(实际没啥用)
     * merge the same topic
     *
     * @param clientId clientId
     * @return TopicPairs
     */
    private Set<TopicPair> mergeTopics(String clientId, boolean enableShared) {
        Set<TopicPair> topicPairs = new HashSet<>();
        for (MqttSubscriber subscriber : MqttSubscriber.list()) {
            if (subscriber.containsClientId(clientId)) {
                topicPairs.addAll(subscriber.getTopics());
            }
        }
        if (topicPairs.isEmpty()) {
            return topicPairs;
        }
        TopicPair[] pairs = new TopicPair[topicPairs.size()];
        for (TopicPair topic : topicPairs) {
            for (int i = 0; i < pairs.length; ++i) {
                TopicPair pair = pairs[i];
                if (pair == null) {
                    pairs[i] = topic;
                    break;
                }
                if (pair.getQos() != topic.getQos()) {
                    continue;
                }
                String temp = pair.getTopic(enableShared)
                        .replace('+', '\u0000')
                        .replace("#", "\u0000/\u0000");
                if (MqttTopic.isMatched(topic.getTopic(enableShared), temp)) {
                    pairs[i] = topic;
                    continue;
                }
                temp = topic.getTopic(enableShared)
                        .replace('+', '\u0000')
                        .replace("#", "\u0000/\u0000");
                if (MqttTopic.isMatched(pair.getTopic(enableShared), temp)) {
                    break;
                }
            }
        }
        return Arrays.stream(pairs).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    @Override
    public void destroy() {
        // 卸载的时候把缓存清空
        log.info("shutting down all mqtt client.");
        MQTT_CLIENT_MAP.forEach((id, client) -> {
            try {
                client.close();
            } catch (Exception e) {
                log.error("mqtt client '{}' close error: {}", id, e.getMessage(), e);
            }
        });
        MQTT_CLIENT_MAP.clear();
        // 清空订阅处理方法缓存
        MqttSubscriber.destroy();
        // 清空类型转换缓存
        MqttConversionService.destroy();
    }
}
