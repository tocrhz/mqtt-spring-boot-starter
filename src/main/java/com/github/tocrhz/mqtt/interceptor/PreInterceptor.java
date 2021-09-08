package com.github.tocrhz.mqtt.interceptor;

import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * 2021/9/7 16:12
 * <p>
 * Pre-interceptor for receiving messages @author WangChenChen
 * </p>
 * @author WangChenChen
 */

public interface PreInterceptor {

    /**
     * 2021/9/7 16:16
     * Receive message pre-interceptor
     * @param clientId 客户端ID
     * @param topic 主题
     * @param mqttMessage 消息
     * @author WangChenChen
     */
    void receiveHandler(String clientId, String topic, MqttMessage mqttMessage);

}
