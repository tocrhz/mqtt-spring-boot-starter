package com.github.tocrhz.mqtt.autoconfigure;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

/**
 * Alter MqttConnectOptions
 *
 * @author tocrhz
 */
public abstract class MqttConnectOptionsAdapter {
    /**
     * Alter MqttConnectOptions
     *
     * @param clientId 客户端ID
     * @param options  MqttConnectOptions
     */
    protected abstract void configure(String clientId, MqttConnectOptions options);
}
