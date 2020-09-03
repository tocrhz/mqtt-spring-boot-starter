package com.github.tocrhz.mqtt.autoconfigure;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

/**
 * Alter MqttConnectOptions
 *
 * @author tocrhz
 */
@FunctionalInterface
public interface MqttConnectOptionsAdapter {
    /**
     * Alter MqttConnectOptions
     *
     * @param clientId 客户端ID
     * @param options  MqttConnectOptions
     */
    void configure(String clientId, MqttConnectOptions options);
}
