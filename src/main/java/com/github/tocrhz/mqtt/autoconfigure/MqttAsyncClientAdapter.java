package com.github.tocrhz.mqtt.autoconfigure;

import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttException;

/**
 * Create mqtt async client
 *
 * @author tocrhz
 */
public abstract class MqttAsyncClientAdapter {
    /**
     * Create mqtt async client
     *
     * @param clientId  client ID
     * @param serverURIs serverURIs, String[]
     * @return IMqttAsyncClient
     */
    protected abstract IMqttAsyncClient create(String clientId, String[] serverURIs) throws MqttException;
}
