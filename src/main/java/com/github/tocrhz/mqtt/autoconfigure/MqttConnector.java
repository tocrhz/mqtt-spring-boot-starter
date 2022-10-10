package com.github.tocrhz.mqtt.autoconfigure;

import com.github.tocrhz.mqtt.properties.MqttProperties;
import com.github.tocrhz.mqtt.subscriber.MqttSubscriber;
import com.github.tocrhz.mqtt.subscriber.TopicPair;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Establish a connection and subscribe to topics.
 * <p>
 * 排序为{@link org.springframework.core.Ordered#LOWEST_PRECEDENCE} 保证最后初始化
 *
 * @author tocrhz
 */
public class MqttConnector implements DisposableBean {
    private final static Logger log = LoggerFactory.getLogger(MqttConnector.class);
    public final Map<String, IMqttAsyncClient> mqttClientMap = new HashMap<>();
    public final Map<String, Integer> mqttDefaultQosMap = new HashMap<>();

    private List<MqttSubscriber> subscribers = new ArrayList<>();
    public static String DefaultClientId;
    public static int DefaultPublishQos;

    public IMqttAsyncClient getDefaultClient() {
        if (StringUtils.hasText(DefaultClientId)) {
            return mqttClientMap.get(DefaultClientId);
        } else if (!mqttClientMap.isEmpty()) {
            return mqttClientMap.values().iterator().next();
        }
        return null;
    }

    public int getDefaultQosById(String clientId) {
        if (StringUtils.hasText(clientId)) {
            return mqttDefaultQosMap.getOrDefault(clientId, 0);
        } else {
            return DefaultPublishQos;
        }
    }

    /**
     * Get from {@link MqttConnector#mqttClientMap} by client id.
     * <p>
     * Call {@link MqttConnector#getDefaultClient()} if client id is if {@code null}.
     *
     * @param clientId id
     * @return IMqttAsyncClient
     * @see MqttConnector#getDefaultClient()
     */
    public IMqttAsyncClient getClientById(String clientId) {
        if (StringUtils.hasText(clientId)) {
            return mqttClientMap.get(clientId);
        } else {
            return getDefaultClient();
        }
    }

    // for reconnect
    private final ScheduledExecutorService scheduled = Executors.newScheduledThreadPool(2);
    private MqttProperties properties;
    private MqttConfigurer adapter;

    public void start(MqttProperties properties, MqttConfigurer adapter) {
        if (properties.getDisable() == null || !properties.getDisable()) {
            adapter.setProperties(properties);
            // create clients
            this.properties = properties;
            this.adapter = adapter;
            this.connect();
        }
    }

    /**
     * 根据配置建立连接.
     */
    public void connect() {
        connect(false);
    }

    /**
     * 根据配置建立连接.
     *
     * @param force 强制建立新的连接，如果存在旧连接则断开.
     */
    public void connect(boolean force) {
        properties.forEach((id, options) -> {
            try {
                if (mqttClientMap.containsKey(id)) {
                    if (force) {
                        disconnect(id);
                    } else {
                        return;
                    }
                }
                IMqttAsyncClient client = adapter.postCreate(id, options);
                if (client != null) {
                    mqttClientMap.put(client.getClientId(), client);
                    mqttDefaultQosMap.put(client.getClientId(), properties.getDefaultPublishQos(client.getClientId()));
                    if (!StringUtils.hasText(DefaultClientId)) {
                        DefaultClientId = client.getClientId();
                        DefaultPublishQos = mqttDefaultQosMap.get(client.getClientId());
                        log.info("Default mqtt client is '{}'", DefaultClientId);
                    }
                    // connect to mqtt server.
                    scheduled.schedule(new ReConnect(client, options), 1, TimeUnit.MILLISECONDS);
                }
            } catch (MqttException exception) {
                exception.printStackTrace();
            }
        });
    }

    /**
     * 建立连接.
     */
    private void connect(IMqttAsyncClient client, MqttConnectOptions options) {
        try {
            client.connect(options, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    try {
                        log.info("Connect success. client_id is [{}], brokers is [{}]."
                                , client.getClientId()
                                , String.join(",", options.getServerURIs()));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    try {
                        log.error("Connect failure. client_id is [{}], brokers is [{}]. retry after {} ms."
                                , client.getClientId()
                                , String.join(",", options.getServerURIs())
                                , options.getMaxReconnectDelay());
                        scheduled.schedule(new ReConnect(client, options), options.getMaxReconnectDelay(), TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            client.setCallback(new MqttCallbackExtended() {
                private final String clientId = client.getClientId();

                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    if (reconnect) {
                        log.info("Mqtt reconnection success.");
                        resubscribe(client);
                    }
                }

                @Override
                public void connectionLost(Throwable cause) {
                    log.warn("Mqtt connection lost.");
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    for (MqttSubscriber subscriber : subscribers) {
                        subscriber.accept(clientId, topic, message);
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    // 重新连接
    private void resubscribe(IMqttAsyncClient client) {

    }

    /**
     * 关闭指定的客户端.
     */
    public void disconnect(String clientId) {
        Assert.notNull(clientId, "disconnect client id can not be null.");
        try {
            IMqttAsyncClient client = this.mqttClientMap.get(clientId);
            client.disconnect();
            this.mqttClientMap.remove(clientId);
        } catch (MqttException ignored) {
        }
        if (clientId.equals(MqttConnector.DefaultClientId)) {
            if (this.mqttClientMap.size() > 0) {
                MqttConnector.DefaultClientId = this.mqttClientMap.keySet().iterator().next();
            } else {
                MqttConnector.DefaultClientId = null;
            }
        }
    }

    /**
     * 订阅.
     */
    public void subscribe(MqttSubscriber subscriber) {
        if (subscriber.getClientIds() == null || subscriber.getClientIds().length == 0) {
            this.subscribe(null, subscriber);
        }
        for (String clientId : subscriber.getClientIds()) {
            this.subscribe(clientId, subscriber);
        }
        this.subscribers.add(subscriber);
    }

    public void subscribe(String clientId, MqttSubscriber subscriber) {
        IMqttAsyncClient iMqttAsyncClient = Optional.ofNullable(this.mqttClientMap.get(clientId)).orElse(this.getDefaultClient());
        boolean sharedEnable = this.properties.isSharedEnable(iMqttAsyncClient.getClientId());
        if (iMqttAsyncClient == null) {
            log.warn("There is no mqtt async client has been found {}'.", iMqttAsyncClient.getClientId());
            return;
        }

        try {
            Set<TopicPair> topicPairs = mergeTopics(sharedEnable, subscriber);
            this.adapter.beforeSubscribe(iMqttAsyncClient.getClientId(), topicPairs);
            if (topicPairs.isEmpty()) {
                log.warn("There is no topic has been found for client '{}'.", iMqttAsyncClient.getClientId());
                return;
            }
            StringJoiner sj = new StringJoiner(",");
            String[] topics = new String[topicPairs.size()];
            int[] QOSs = new int[topicPairs.size()];
            int i = 0;
            for (TopicPair topicPair : topicPairs) {
                topics[i] = topicPair.getTopic(sharedEnable);
                QOSs[i] = topicPair.getQos();
                sj.add("('" + topics[i] + "', " + QOSs[i] + ")");
                ++i;
            }
            iMqttAsyncClient.subscribe(topics, QOSs);
            log.info("Mqtt client '{}' subscribe success. topics : " + sj, iMqttAsyncClient.getClientId());
        } catch (MqttException e) {
            log.error("Mqtt client '{}' subscribe failure.", iMqttAsyncClient.getClientId(), e);
        }
    }

    /**
     * 合并相似的主题(实际没啥用)
     * merge the same topic
     *
     * @return TopicPairs
     */
    private Set<TopicPair> mergeTopics(boolean sharedEnable, MqttSubscriber subscriber) {
        Set<TopicPair> topicPairs = new HashSet<>();
        topicPairs.addAll(subscriber.getTopics());
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
                String temp = pair.getTopic(sharedEnable)
                        .replace('+', '\u0000')
                        .replace("#", "\u0000/\u0000");
                if (MqttTopic.isMatched(topic.getTopic(sharedEnable), temp)) {
                    pairs[i] = topic;
                    continue;
                }
                temp = topic.getTopic(sharedEnable)
                        .replace('+', '\u0000')
                        .replace("#", "\u0000/\u0000");
                if (MqttTopic.isMatched(pair.getTopic(sharedEnable), temp)) {
                    break;
                }
            }
        }
        return Arrays.stream(pairs).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    @Override
    public void destroy() {
        log.info("Shutting down mqtt clients.");
        mqttClientMap.forEach((id, client) -> {
            try {
                if (client.isConnected()) {
                    client.disconnect();
                }
            } catch (Exception e) {
                log.error("Mqtt disconnect error: {}", e.getMessage(), e);
            }
            try {
                client.close();
            } catch (Exception e) {
                log.error("Mqtt close error: {}", e.getMessage(), e);
            }
        });
        mqttClientMap.clear();
    }

    private class ReConnect implements Runnable {

        final IMqttAsyncClient client;
        final MqttConnectOptions options;

        ReConnect(IMqttAsyncClient client, MqttConnectOptions options) {
            this.client = client;
            this.options = options;
        }

        @Override
        public void run() {
            connect(client, options);
        }
    }
}