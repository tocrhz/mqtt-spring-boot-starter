package com.github.tocrhz.mqtt.autoconfigure;

import com.github.tocrhz.mqtt.properties.MqttProperties;
import com.github.tocrhz.mqtt.publisher.MqttPublisher;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * mqtt auto configuration
 *
 * @author tocrhz
 */
@Order(1010)
@AutoConfigureAfter(PayloadAutoConfiguration.class)
@ConditionalOnClass(MqttAsyncClient.class)
@ConditionalOnProperty(prefix = "mqtt", name = "disable", havingValue = "false", matchIfMissing = true)
@EnableConfigurationProperties(MqttProperties.class)
@Configuration
public class MqttAutoConfiguration {


    public MqttAutoConfiguration(ListableBeanFactory beanFactory) {
        // register converters
        MqttConversionService.addBeans(MqttConversionService.getSharedInstance(), beanFactory);
    }
    public static void initialization(MqttProperties properties) {
        properties.forEach((id, options) -> {
            try {
                MqttAsyncClient client = newMqttClient(id, options.getServerURIs());
                MQTT_OPTIONS_MAP.put(id, options);
                MQTT_CLIENT_MAP.put(id, client);
            } catch (MqttException exception) {
                exception.printStackTrace();
            }
        });
    }

    /**
     * default MqttConnectOptionsAdapter, nothing to do.
     */
    @Bean
    @Order(1010)
    @ConditionalOnMissingBean(MqttConnectOptionsAdapter.class)
    public MqttConnectOptionsAdapter mqttConnectOptionsAdapter() {
        return new MqttConnectOptionsAdapter() {
            @Override
            protected void configure(String clientId, MqttConnectOptions options) {
            }
        };
    }

    /**
     * default MqttClientPersistence, MemoryPersistence
     */
    @Bean
    @Order(1010)
    @ConditionalOnMissingBean(MqttClientWrapper.class)
    public MqttClientWrapper mqttClientWrapper(MqttProperties properties) {
        MqttClientWrapper wrapper = new MqttClientWrapper();

        return wrapper;
    }

    /**
     * default MqttPublisher
     */
    @Bean
    @Order(1013)
    @ConditionalOnMissingBean(MqttPublisher.class)
    public MqttPublisher mqttPublisher(MqttClientWrapper clientWrapper) throws MqttException {
        return new MqttPublisher(clientWrapper);
    }

    /**
     * default MqttConnector.
     * <p>
     * Ensure the final initialization, the order is {@link org.springframework.core.Ordered#LOWEST_PRECEDENCE}
     */
    @Bean
    @Order // Ordered.LOWEST_PRECEDENCE
    @ConditionalOnMissingBean(MqttConnector.class)
    public MqttConnector mqttConnector(MqttProperties properties, MqttConnectOptionsAdapter adapter, MqttClientPersistence persistence) throws MqttException {
        MqttConnector connector = new MqttConnector(clientInstance(properties, persistence), properties, adapter);
        connector.start();
        return connector;
    }
}
