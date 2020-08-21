package com.github.tocrhz.mqtt.autoconfigure;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

/**
 * extension
 *
 * @author tocrhz
 */
public abstract class MqttConnectOptionsAdapter {
    protected abstract void configure(MqttConnectOptions options);
}
