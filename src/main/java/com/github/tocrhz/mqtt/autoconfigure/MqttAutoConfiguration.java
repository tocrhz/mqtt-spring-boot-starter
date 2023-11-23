package com.github.tocrhz.mqtt.autoconfigure;

import com.github.tocrhz.mqtt.convert.MqttConversionService;
import com.github.tocrhz.mqtt.properties.MqttConfigAdapter;
import com.github.tocrhz.mqtt.properties.MqttProperties;
import com.github.tocrhz.mqtt.publisher.MqttPublisher;
import com.github.tocrhz.mqtt.subscriber.MqttSubscriber;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.LinkedList;

/**
 * mqtt auto configuration
 *
 * @author tocrhz
 */
@Order
@AutoConfigureAfter(PayloadJacksonAutoConfiguration.class)
@ConditionalOnClass(MqttAsyncClient.class)
@ConditionalOnProperty(prefix = "mqtt", name = "disable", havingValue = "false", matchIfMissing = true)
@EnableConfigurationProperties(MqttProperties.class)
@Configuration
public class MqttAutoConfiguration {

    private final ConfigurableBeanFactory factory;

    public MqttAutoConfiguration(ListableBeanFactory beanFactory, ConfigurableBeanFactory factory) {
        // register converters
        MqttConversionService.addBeans(beanFactory);
        this.factory = factory;
    }


    /**
     * 若没有 MqttConfigurer 则创建一个空的
     */
    @Bean
    @ConditionalOnMissingBean(MqttConfigAdapter.class)
    public MqttConfigAdapter mqttConfigAdapter() {
        return MqttConfigAdapter.defaultAdapter();
    }

    /**
     * default MqttConnector.
     * <p>
     * Ensure the final initialization, the order is {@link org.springframework.core.Ordered#LOWEST_PRECEDENCE}
     *
     * @param adapter    MqttConfigurer
     * @param properties MqttProperties
     * @return MqttConnector
     */
    @Bean
    @Order
    @ConditionalOnMissingBean(MqttClientManager.class)
    public MqttClientManager mqttClientManager(MqttSubscribeProcessor processor, MqttProperties properties, MqttConfigAdapter adapter) {

        LinkedList<MqttSubscriber> subscribers = processor.getSubscribers();
        // init property before connected.
        adapter.beforeResolveEmbeddedValue(subscribers);
        for (MqttSubscriber subscriber : subscribers) {
            subscriber.resolveEmbeddedValue(factory);
        }
        adapter.afterResolveEmbeddedValue(subscribers);
        MqttClientManager manager = new MqttClientManager(subscribers, properties, adapter);
        // 将mqtt客户端添加进去
        properties.forEach(manager::clientNew);
        // 建立连接
        manager.afterInit();
        return manager;
    }


    /**
     * default MqttPublisher
     *
     * @return MqttPublisher
     */
    @Bean
    @Order
    @ConditionalOnMissingBean(MqttPublisher.class)
    public MqttPublisher mqttPublisher(MqttClientManager manager) {
        return new MqttPublisher(manager);
    }
}
