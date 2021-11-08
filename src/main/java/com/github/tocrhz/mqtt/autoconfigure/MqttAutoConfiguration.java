package com.github.tocrhz.mqtt.autoconfigure;

import com.github.tocrhz.mqtt.properties.MqttProperties;
import com.github.tocrhz.mqtt.publisher.MqttPublisher;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
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
@AutoConfigureAfter(PayloadJacksonAutoConfiguration.class)
@ConditionalOnClass(MqttAsyncClient.class)
@ConditionalOnProperty(prefix = "mqtt", name = "disable", havingValue = "false", matchIfMissing = true)
@EnableConfigurationProperties(MqttProperties.class)
@Configuration
public class MqttAutoConfiguration {

    public MqttAutoConfiguration(ListableBeanFactory beanFactory) {
        // register converters
        MqttConversionService.addBeans(MqttConversionService.getSharedInstance(), beanFactory);
    }


    @Bean
    @Order(1010)
    @ConditionalOnMissingBean(MqttConfigurer.class)
    public MqttConfigurer mqttConfigurer() {
        return new MqttConfigurer() {
        };
    }


    /**
     * default MqttPublisher
     *
     * @return MqttPublisher
     */
    @Bean
    @Order(1013)
    @ConditionalOnMissingBean(MqttPublisher.class)
    public MqttPublisher mqttPublisher() {
        return new MqttPublisher();
    }

    /**
     * default MqttConnector.
     * <p>
     * Ensure the final initialization, the order is {@link org.springframework.core.Ordered#LOWEST_PRECEDENCE}
     *
     * @param configurer    MqttConfigurer
     * @param properties    MqttProperties
     * @return MqttConnector
     */
    @Bean
    @Order // Ordered.LOWEST_PRECEDENCE
    public MqttConnector mqttConnector(MqttProperties properties, MqttConfigurer configurer) {
        MqttConnector connector = new MqttConnector();
        connector.start(properties, configurer);
        return connector;
    }
}
