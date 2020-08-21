package com.github.tocrhz.mqtt.autoconfigure;

import com.github.tocrhz.mqtt.subscriber.MqttSubscriber;
import com.github.tocrhz.mqtt.subscriber.TopicPair;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Establish a connection and subscribe to topics.
 *
 *
 * 排序为{@link org.springframework.core.Ordered#LOWEST_PRECEDENCE} 保证最后初始化
 *
 * @author tocrhz
 */
public class MqttConnector implements DisposableBean {
    private final Logger log = LoggerFactory.getLogger(MqttConnector.class);

    private final MqttAsyncClient client;
    private final MqttProperties properties;
    private final MqttConnectOptionsAdapter adapter;

    public MqttConnector(MqttAsyncClient client, MqttProperties properties, MqttConnectOptionsAdapter adapter) {
        this.client = client;
        this.properties = properties;
        this.adapter = adapter;
    }

    public void start() {
        if (properties.getDisable() == null || !properties.getDisable()) {
            // sort by order.
            MqttSubscribeProcessor.SUBSCRIBERS.sort(Comparator.comparingInt(MqttSubscriber::getOrder));
            // connect to mqtt server.
            connect();
        }
    }

    private void connect() {
        try {
            MqttConnectOptions options = properties.toOptions();
            adapter.configure(options);
            client.connect(options, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    try {
                        log.info("connect mqtt success. brokers is [{}] client_id is [{}]."
                                , String.join(",", properties.getUri())
                                , properties.getClientId());
                        subscribe();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    try {
                        log.error("connect mqtt failure. brokers is [{}] client_id is [{}]."
                                , String.join(",", properties.getUri())
                                , properties.getClientId());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
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
                    for (MqttSubscriber subscriber : MqttSubscribeProcessor.SUBSCRIBERS) {
                        try {
                            subscriber.accept(topic, message);
                        } catch (Exception e) {
                            log.error("mqtt subscriber error.", e);
                        }
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

    private void subscribe() {
        try {
            Set<TopicPair> topicPairs = mergeTopics();
            if (topicPairs.isEmpty()) {
                log.warn("there is no topic has been find.");
                return;
            }
            StringJoiner sj = new StringJoiner(",");
            String[] topics = new String[topicPairs.size()];
            int[] QOSs = new int[topicPairs.size()];
            int i = 0;
            for (TopicPair topicPair : topicPairs) {
                topics[i] = topicPair.getTopic();
                QOSs[i] = topicPair.getQos();
                sj.add("('" + topics[i] + "', " + QOSs[i] + ")");
                ++i;
            }
            client.subscribe(topics, QOSs);
            log.info("subscribe success. topics : " + sj.toString());
        } catch (MqttException e) {
            log.error("subscribe failure.", e);
        }
    }

    private Set<TopicPair> mergeTopics() {
        Set<TopicPair> topicPairs = new HashSet<>();
        for (MqttSubscriber subscriber : MqttSubscribeProcessor.SUBSCRIBERS) {
            topicPairs.addAll(subscriber.getTopics());
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
                String temp = pair.getTopic()
                        .replace('+', '\u0000')
                        .replace("#", "\u0000/\u0000");
                if (MqttTopic.isMatched(topic.getTopic(), temp)) {
                    pairs[i] = topic;
                    continue;
                }
                temp = topic.getTopic()
                        .replace('+', '\u0000')
                        .replace("#", "\u0000/\u0000");
                if (MqttTopic.isMatched(pair.getTopic(), temp)) {
                    break;
                }
            }
        }
        return Arrays.stream(pairs).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    @Override
    public void destroy() {
        log.info("Shutting down mqtt.");
        try {
            if (client.isConnected()) {
                client.disconnect();
            }
        } catch (Exception e) {
            log.error("mqtt disconnect error: {}", e.getMessage(), e);
        }
        try {
            client.close();
        } catch (Exception e) {
            log.error("mqtt close error: {}", e.getMessage(), e);
        }
    }
}