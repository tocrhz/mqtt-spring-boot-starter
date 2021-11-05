package com.github.tocrhz.mqtt.autoconfigure;

import com.github.tocrhz.mqtt.properties.MqttProperties;

public interface MqttConfigurer {
    /**
     * Configuration the properties before client init.
     *
     * @param properties MqttProperties
     */
    void configure(MqttProperties properties);
}
