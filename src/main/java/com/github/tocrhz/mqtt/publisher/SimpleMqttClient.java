package com.github.tocrhz.mqtt.publisher;

import com.github.tocrhz.mqtt.convert.MqttConversionService;
import com.github.tocrhz.mqtt.properties.MqttConfigAdapter;
import com.github.tocrhz.mqtt.subscriber.MqttSubscriber;
import com.github.tocrhz.mqtt.subscriber.TopicPair;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 简单封装下客户端
 */
public record SimpleMqttClient(String id, MqttConnectOptions options
        , IMqttAsyncClient client, Set<TopicPair> topics, boolean enableShared, int qos, MqttConfigAdapter adapter) {
    private static final Logger log = LoggerFactory.getLogger(SimpleMqttClient.class);
    public static final ScheduledExecutorService scheduled = Executors.newScheduledThreadPool(2);

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
                    for (MqttSubscriber subscriber : MqttSubscriber.SUBSCRIBERS) {
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

    private void subscribe() {
        try {
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
        Assert.isTrue(topic != null && !topic.isBlank(), "topic cannot be blank.");
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