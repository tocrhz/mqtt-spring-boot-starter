package com.github.tocrhz.mqtt.publisher;

import com.github.tocrhz.mqtt.autoconfigure.MqttClientManager;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;

/**
 * Used to publish message
 *
 * @author tocrhz
 */
@SuppressWarnings("unused")
public class MqttPublisher {
    private final MqttClientManager manager;

    public MqttPublisher(MqttClientManager manager) {
        this.manager = manager;
    }

    public SimpleMqttClient client() {
        return manager.clientGetOrDefault(null);
    }

    public SimpleMqttClient client(String clientId) {
        return manager.clientGetOrDefault(clientId);
    }

    public void send(String topic, Object payload) {
        client().send(topic, payload);
    }

    public void send(String topic, Object payload, boolean retained) {
        client().send(topic, payload, retained);
    }

    public void send(String topic, Object payload, IMqttActionListener callback) {
        client().send(topic, payload, callback);
    }

    public void send(String topic, Object payload, boolean retained, IMqttActionListener callback) {
        client().send(topic, payload, retained, callback);
    }

    public void send(String topic, Object payload, int qos) {
        client().send(topic, payload, qos);
    }

    public void send(String topic, Object payload, int qos, boolean retained) {
        client().send(topic, payload, qos, retained);
    }

    public void send(String topic, Object payload, int qos, IMqttActionListener callback) {
        client().send(topic, payload, qos, callback);
    }

    public void send(String topic, Object payload, int qos, boolean retained, IMqttActionListener callback) {
        client().send(topic, payload, qos, retained, callback);
    }
}
