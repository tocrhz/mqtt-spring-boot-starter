package com.github.tocrhz.mqtt.autoconfigure;

import com.github.tocrhz.mqtt.properties.MqttProperties;
import com.github.tocrhz.mqtt.subscriber.MqttSubscriber;
import com.github.tocrhz.mqtt.subscriber.TopicPair;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
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
    public final static Map<String, IMqttAsyncClient> MQTT_CLIENT_MAP = new HashMap<>();
    public static String DefaultClientId;

    public static IMqttAsyncClient getDefaultClient() {
        if (StringUtils.hasLength(DefaultClientId)) {
            return MQTT_CLIENT_MAP.get(DefaultClientId);
        } else if (!MQTT_CLIENT_MAP.isEmpty()) {
            return MQTT_CLIENT_MAP.values().iterator().next();
        }
        return null;
    }

    /**
     * Get from {@link MqttConnector#MQTT_CLIENT_MAP} by client id.
     * <p>
     * Call {@link MqttConnector#getDefaultClient()} if client id is if {@code null}.
     *
     * @param clientId id
     * @return IMqttAsyncClient
     * @see MqttConnector#getDefaultClient()
     */
    public static IMqttAsyncClient getClientById(String clientId) {
        if (StringUtils.hasLength(clientId)) {
            return MQTT_CLIENT_MAP.get(clientId);
        } else {
            return getDefaultClient();
        }
    }

    // for reconnect
    private final ScheduledExecutorService scheduled = Executors.newScheduledThreadPool(2);
    private MqttProperties properties;
    public void start(MqttAsyncClientAdapter clientAdapter, MqttProperties properties, MqttConnectOptionsAdapter adapter) {
        if (properties.getDisable() == null || !properties.getDisable()) {
            // sort subscribe by order.
            MqttSubscribeProcessor.SUBSCRIBERS.sort(Comparator.comparingInt(MqttSubscriber::getOrder));
            // create clients
            this.properties = properties;
            properties.forEach((id, options) -> {
                try {
                    adapter.configure(id, options);
                    IMqttAsyncClient client = clientAdapter.create(id, options.getServerURIs());
                    if (client != null) {
                        if (!StringUtils.hasLength(DefaultClientId)) {
                            DefaultClientId = id;
                            log.info("Default mqtt client is '{}'", DefaultClientId);
                        }
                        // put to map
                        MQTT_CLIENT_MAP.put(id, client);
                        // connect to mqtt server.
                        scheduled.schedule(new ReConnect(client, options), 1, TimeUnit.MILLISECONDS);
                    }
                } catch (MqttException exception) {
                    exception.printStackTrace();
                }
            });
        }
    }

    private void connect(IMqttAsyncClient client, MqttConnectOptions options) {
        try {
            client.connect(options, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    try {
                        log.info("Connect success. client_id is [{}], brokers is [{}]."
                                , client.getClientId()
                                , String.join(",", options.getServerURIs()));
                        subscribe(client);
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
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    if (reconnect) {
                        log.info("Mqtt reconnection success.");
                        subscribe(client);
                    }
                }

                @Override
                public void connectionLost(Throwable cause) {
                    log.warn("Mqtt connection lost.");
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    for (MqttSubscriber subscriber : MqttSubscribeProcessor.SUBSCRIBERS) {
                        try {
                            subscriber.accept(topic, message);
                        } catch (Exception e) {
                            log.error("Mqtt subscriber process error.", e);
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

    private void subscribe(IMqttAsyncClient client) {
        String clientId = client.getClientId();
        boolean sharedSubscription = this.properties.isSharedSubscription(clientId);
        try {
            Set<TopicPair> topicPairs = mergeTopics(clientId, sharedSubscription);
            if (topicPairs.isEmpty()) {
                log.warn("There is no topic has been found for client '{}'.", clientId);
                return;
            }
            StringJoiner sj = new StringJoiner(",");
            String[] topics = new String[topicPairs.size()];
            int[] QOSs = new int[topicPairs.size()];
            int i = 0;
            for (TopicPair topicPair : topicPairs) {
                topics[i] = topicPair.getTopic(sharedSubscription);
                QOSs[i] = topicPair.getQos();
                sj.add("('" + topics[i] + "', " + QOSs[i] + ")");
                ++i;
            }
            client.subscribe(topics, QOSs);
            log.info("Mqtt client '{}' subscribe success. topics : " + sj.toString(), clientId);
        } catch (MqttException e) {
            log.error("Mqtt client '{}' subscribe failure.", clientId, e);
        }
    }

    /**
     * merge the same topic
     *
     * @param clientId clientId
     * @return TopicPairs
     */
    private Set<TopicPair> mergeTopics(String clientId, boolean sharedSubscription) {
        Set<TopicPair> topicPairs = new HashSet<>();
        for (MqttSubscriber subscriber : MqttSubscribeProcessor.SUBSCRIBERS) {
            if (subscriber.contains(clientId)) {
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
                String temp = pair.getTopic(sharedSubscription)
                        .replace('+', '\u0000')
                        .replace("#", "\u0000/\u0000");
                if (MqttTopic.isMatched(topic.getTopic(sharedSubscription), temp)) {
                    pairs[i] = topic;
                    continue;
                }
                temp = topic.getTopic(sharedSubscription)
                        .replace('+', '\u0000')
                        .replace("#", "\u0000/\u0000");
                if (MqttTopic.isMatched(pair.getTopic(sharedSubscription), temp)) {
                    break;
                }
            }
        }
        return Arrays.stream(pairs).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    @Override
    public void destroy() {
        log.info("Shutting down mqtt clients.");
        MQTT_CLIENT_MAP.forEach((id, client) -> {
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
        MQTT_CLIENT_MAP.clear();
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