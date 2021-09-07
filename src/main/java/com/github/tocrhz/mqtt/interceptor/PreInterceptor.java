package com.github.tocrhz.mqtt.interceptor;

import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * <p>
 * Pre-interceptor for receiving messages
 * </p>
 *
 * @author WangChenChen
 * @date 2021/9/7 16:12
 */

public interface PreInterceptor {

    /**
     * Receive message pre-interceptor
     *
     * @param clientId
     * @param topic
     * @param mqttMessage
     * @author WangChenChen
     * @date 2021/9/7 16:16
     */
    void receiveHandler(String clientId, String topic, MqttMessage mqttMessage);

}
