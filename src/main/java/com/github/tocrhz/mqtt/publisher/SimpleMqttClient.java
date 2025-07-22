package com.github.tocrhz.mqtt.publisher;

import com.github.tocrhz.mqtt.convert.MqttConversionService;
import com.github.tocrhz.mqtt.properties.MqttConfigAdapter;
import com.github.tocrhz.mqtt.subscriber.MqttSubscriber;
import com.github.tocrhz.mqtt.subscriber.TopicPair;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 简单封装下客户端
 */
public class SimpleMqttClient {

    private final String id;
    private final IMqttAsyncClient client;
    private final MqttConnectOptions options;
    private final boolean enableShared;
    private final int qos;
    private final ArrayList<MqttSubscriber> subscribers;
    private final MqttConfigAdapter adapter;

    public String id() {
        return this.id;
    }

    public IMqttAsyncClient client() {
        return this.client;
    }

    public MqttConnectOptions options() {
        return this.options;
    }

    public boolean enableShared() {
        return this.enableShared;
    }

    public int qos() {
        return this.qos;
    }

    public ArrayList<MqttSubscriber> subscribers() {
        return this.subscribers;
    }

    public MqttConfigAdapter adapter() {
        return this.adapter;
    }

    /**
     * 简单封装下客户端
     *
     * @param id           客户端ID
     * @param client       客户端
     * @param options      连接选项
     * @param enableShared 是否支持共享订阅
     * @param qos          默认的发布QOS
     * @param subscribers  处理消息的方法集合
     * @param adapter      扩展
     */
    public SimpleMqttClient(String id, IMqttAsyncClient client, MqttConnectOptions options
            , boolean enableShared, int qos
            , ArrayList<MqttSubscriber> subscribers
            , MqttConfigAdapter adapter) {
        this.id = id;
        this.client = client;
        this.options = options;
        this.enableShared = enableShared;
        this.qos = qos;
        this.subscribers = subscribers;
        this.adapter = adapter;
    }

    private static final Logger log = LoggerFactory.getLogger(SimpleMqttClient.class);
    private static final ScheduledExecutorService scheduled = Executors.newSingleThreadScheduledExecutor();

    public void connect() {
        try {
            adapter.beforeConnect(id, options);
            client.connect(options, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    log.info("connect success. client_id is [{}], brokers is [{}].", id, String.join(",", options.getServerURIs()));
                    subscribe();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    log.error("connect failure. client_id is [{}], brokers is [{}]. retry after {} ms."
                            , id, String.join(",", options.getServerURIs()), options.getMaxReconnectDelay());
                    scheduled.schedule(() -> connect(), options.getMaxReconnectDelay(), TimeUnit.MILLISECONDS);
                }
            });
            client.setCallback(new MqttCallbackExtended() {

                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    if (reconnect) {
                        log.info("mqtt reconnection success.");
                        subscribe();
                    }
                }

                @Override
                public void connectionLost(Throwable cause) {
                    log.warn("mqtt connection lost.");
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    for (MqttSubscriber subscriber : subscribers) {
                        subscriber.accept(id, topic, message);
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });
        } catch (MqttException e) {
            log.error("connect error: {}", e.getMessage(), e);
        }
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
        for (MqttSubscriber subscriber : subscribers) {
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

    private void subscribe() {
        try {
            Set<TopicPair> topics = mergeTopics(id, enableShared);
            this.adapter.beforeSubscribe(id, topics);

            if (topics.isEmpty()) {
                log.info("there is no topic has been found for client '{}'.", id);
            } else {
                StringJoiner sj = new StringJoiner(",");
                String[] topic = new String[topics.size()];
                int[] qos = new int[topics.size()];
                int i = 0;
                for (TopicPair pair : topics) {
                    topic[i] = pair.getTopic(enableShared);
                    qos[i] = pair.getQos();
                    sj.add("('" + topic[i] + "', " + qos[i] + ")");
                    ++i;
                }
                client.subscribe(topic, qos);
                log.info("mqtt client '{}' subscribe success. topics : " + sj, id);
            }
        } catch (MqttException e) {
            log.error("mqtt client '{}' subscribe failure.", id, e);
        }
    }

    public void close() {
        try (IMqttAsyncClient imac = client()) {
            if (imac.isConnected()) {
                imac.disconnect();
            }
        } catch (MqttException e) {
            log.error("mqtt client '{}' disconnect error: {}", id, e.getMessage(), e);
        }
    }


    public void send(String topic, Object payload) {
        send(topic, payload, qos(), false, null);
    }

    public void send(String topic, Object payload, boolean retained) {
        send(topic, payload, qos(), retained, null);
    }

    public void send(String topic, Object payload, IMqttActionListener callback) {
        send(topic, payload, qos(), false, callback);
    }

    public void send(String topic, Object payload, boolean retained, IMqttActionListener callback) {
        send(topic, payload, qos(), retained, callback);
    }

    public void send(String topic, Object payload, int qos) {
        send(topic, payload, qos, false, null);
    }

    public void send(String topic, Object payload, int qos, boolean retained) {
        send(topic, payload, qos, retained, null);
    }

    public void send(String topic, Object payload, int qos, IMqttActionListener callback) {
        send(topic, payload, qos, false, callback);
    }

    /**
     * 发送消息到指定主题, 指定qos, retained
     *
     * @param topic    主题
     * @param payload  消息内容
     * @param qos      服务质量
     * @param retained 保留消息
     * @param callback 消息发送完成后的回调
     * @throws IllegalArgumentException if topic is empty
     * @throws NullPointerException     if client not exists
     */
    public void send(String topic, Object payload, int qos, boolean retained, IMqttActionListener callback) {
        Assert.isTrue(topic != null && !topic.isEmpty(), "topic cannot be blank.");
        byte[] bytes = MqttConversionService.getSharedInstance().toBytes(payload);
        if (bytes == null) {
            return;
        }
        MqttMessage message = toMessage(bytes, qos, retained);
        try {
            client.publish(topic, message, null, callback);
        } catch (Throwable throwable) {
            log.error("message publish error: {}", throwable.getMessage(), throwable);
        }
    }

    private MqttMessage toMessage(byte[] payload, int qos, boolean retained) {
        MqttMessage message = new MqttMessage();
        message.setPayload(payload);
        message.setQos(qos);
        message.setRetained(retained);
        return message;
    }
}