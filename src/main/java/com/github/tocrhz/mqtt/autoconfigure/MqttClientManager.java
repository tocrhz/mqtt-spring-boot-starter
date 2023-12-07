package com.github.tocrhz.mqtt.autoconfigure;

import com.github.tocrhz.mqtt.convert.MqttConversionService;
import com.github.tocrhz.mqtt.properties.MqttConfigAdapter;
import com.github.tocrhz.mqtt.properties.MqttConnectionProperties;
import com.github.tocrhz.mqtt.properties.MqttProperties;
import com.github.tocrhz.mqtt.publisher.SimpleMqttClient;
import com.github.tocrhz.mqtt.subscriber.MqttSubscriber;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.LinkedList;

/**
 * 客户端连接管理一下
 */
@SuppressWarnings("unused")
public class MqttClientManager implements DisposableBean, ApplicationListener<ApplicationReadyEvent> {
    private final static Logger log = LoggerFactory.getLogger(MqttClientManager.class);
    private final LinkedHashMap<String, SimpleMqttClient> clients = new LinkedHashMap<>();
    private final LinkedList<MqttSubscriber> subscribers;
    private final MqttProperties properties;
    private final MqttConfigAdapter adapter;
    private final ConfigurableBeanFactory factory;
    private String defaultClientId = null;

    public MqttClientManager(MqttProperties properties, MqttConfigAdapter adapter, ConfigurableBeanFactory factory) {
        this.properties = properties;
        this.adapter = adapter;
        this.subscribers = MqttSubscribeProcessor.subscribers();
        this.factory = factory;
        adapter.setProperties(properties);
    }

    public SimpleMqttClient clientNew(MqttConnectionProperties properties) {
        String clientId = properties.getClientId();

        Assert.hasText(clientId, "property clientId is required.");
        Assert.notEmpty(properties.getUri(), "property uri cannot be empty.");
        Assert.hasText(properties.getUri()[0], "property uri is required.");
        if (clients.containsKey(clientId)) {
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
        if (clients.containsKey(clientId)) {
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
        int qos = defaultPublishQos != null ? defaultPublishQos : this.properties.getDefaultPublishQos(clientId);
        // 创建客户端对象
        SimpleMqttClient smc = new SimpleMqttClient(clientId, client, options, enableShared, qos, subscribers, adapter);
        clients.put(clientId, smc);
        return smc;
    }

    public void clientClose(String clientId) {
        if (clients.containsKey(clientId)) {
            if (defaultClientId != null && defaultClientId.equals(clientId)) {
                String oldDefault = defaultClientId;
                String newDefault = null;
                for (SimpleMqttClient value : clients.values()) {
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
            SimpleMqttClient client = clients.remove(clientId);
            client.close();
        }
    }

    public SimpleMqttClient clientGetOrDefault(String clientId) {
        if (StringUtils.hasText(clientId) && clients.containsKey(clientId)) {
            return clients.get(clientId);
        }
        return clients.get(defaultClientId);
    }

    public boolean setDefaultClientId(String clientId) {
        if (StringUtils.hasText(clientId) && clients.containsKey(clientId)) {
            defaultClientId = clientId;
            return true;
        }
        return false;
    }

    void afterInit() {
        // 初始化完成后，全部建立连接
        clients.forEach((id, client) -> {
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

    @Override
    public void destroy() {
        // 卸载的时候把缓存清空
        log.info("shutting down all mqtt client.");
        clients.forEach((id, client) -> {
            try {
                client.close();
            } catch (Exception e) {
                log.error("mqtt client '{}' close error: {}", id, e.getMessage(), e);
            }
        });
        clients.clear();
        // 清空订阅处理方法缓存
        subscribers.clear();
        // 清空类型转换缓存
        MqttConversionService.destroy();
    }

    void resolveEmbeddedValueTopic() {
        // init property before connected.
        adapter.beforeResolveEmbeddedValue(subscribers);
        for (MqttSubscriber subscriber : subscribers) {
            subscriber.resolveEmbeddedValue(factory);
        }
        adapter.afterResolveEmbeddedValue(subscribers);
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // 配置
        resolveEmbeddedValueTopic();
        // 将mqtt客户端添加进去
        properties.forEach(this::clientNew);
        // 建立连接
        afterInit();
    }
}
