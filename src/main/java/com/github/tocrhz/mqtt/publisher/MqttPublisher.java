package com.github.tocrhz.mqtt.publisher;

import com.github.tocrhz.mqtt.autoconfigure.MqttConversionService;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

/**
 * Used to publish message
 *
 * @author tocrhz
 */
public class MqttPublisher {
    private final static Logger log = LoggerFactory.getLogger(MqttPublisher.class);
    private final MqttAsyncClient client;

    public MqttPublisher(MqttAsyncClient client) {
        this.client = client;
    }

    /**
     * 发送消息到指定主题 qos=1
     *
     * @param topic   主题
     * @param payload 消息内容
     * @param <T>     POJO
     * @throws IllegalArgumentException if topic is blank
     */
    public <T> void send(String topic, T payload) {
        send(topic, payload, 1, false);
    }

    /**
     * 发送消息到指定主题, 指定qos
     *
     * @param topic   主题
     * @param payload 消息内容
     * @param qos     服务质量
     * @throws IllegalArgumentException if topic is blank
     */
    public <T> void send(String topic, T payload, int qos) {
        send(topic, payload, qos, false);
    }

    /**
     * 发送消息到指定主题, 指定qos, retained
     *
     * @param topic    主题
     * @param payload  消息内容
     * @param qos      服务质量
     * @param retained 保留消息
     * @throws IllegalArgumentException if topic is blank
     */
    public <T> void send(String topic, T payload, int qos, boolean retained) {
        Assert.isTrue(topic != null && !topic.trim().isEmpty(), "topic cannot be blank.");
        byte[] bytes = MqttConversionService.getSharedInstance().toBytes(payload);
        if (bytes == null || bytes.length == 0) {
            return;
        }
        MqttMessage message = toMessage(bytes, qos, retained);
        try {
            client.publish(topic, message);
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
