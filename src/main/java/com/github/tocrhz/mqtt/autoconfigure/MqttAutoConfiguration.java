package com.github.tocrhz.mqtt.autoconfigure;

import com.github.tocrhz.mqtt.publisher.MqttPublisher;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
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

    /**
     * default MqttConnectOptionsAdapter, nothing to do.
     */
    @Bean
    @Order(1010)
    @ConditionalOnMissingBean(MqttConnectOptionsAdapter.class)
    public MqttConnectOptionsAdapter mqttConnectOptionsAdapter() {
        return new MqttConnectOptionsAdapter() {
            @Override
            protected void configure(MqttConnectOptions options) {
            }
        };
    }

    /**
     * default MqttClientPersistence, MemoryPersistence
     */
    @Bean
    @Order(1010)
    @ConditionalOnMissingBean(MqttClientPersistence.class)
    public MqttClientPersistence mqttClientPersistence() {
        return new MemoryPersistence();
    }

    /**
     * default MqttPublisher
     */
    @Bean
    @Order(1013)
    @ConditionalOnMissingBean(MqttPublisher.class)
    public MqttPublisher mqttPublisher(MqttProperties properties, MqttClientPersistence persistence) throws MqttException {
        return new MqttPublisher(clientInstance(properties, persistence));
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

    /**
     * mqtt client
     */
    private static MqttAsyncClient client = null;

    /**
     * get or create MqttAsyncClient
     */
    public static synchronized MqttAsyncClient clientInstance(MqttProperties properties, MqttClientPersistence persistence) throws MqttException {
        if (client == null) {
            client = new MqttAsyncClient(properties.getUri()[0], properties.getClientId(), persistence);
        }
        return client;
    }
}
