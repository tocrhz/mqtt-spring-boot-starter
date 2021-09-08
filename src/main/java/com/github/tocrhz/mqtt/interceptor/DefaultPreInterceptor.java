package com.github.tocrhz.mqtt.interceptor;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Pre-interceptor for receiving messages
 * </p>
 * 2021/9/7 16:12
 * @author WangChenChen
 */

public class DefaultPreInterceptor implements PreInterceptor {

    private final static Logger log = LoggerFactory.getLogger(DefaultPreInterceptor.class);

    @Override
    public void receiveHandler(String clientId, String topic, MqttMessage mqttMessage) {
        log.debug("receive clientId: [{}] topic: [{}]  Payload: [{}]", clientId, topic, new String(mqttMessage.getPayload()));
    }

}
