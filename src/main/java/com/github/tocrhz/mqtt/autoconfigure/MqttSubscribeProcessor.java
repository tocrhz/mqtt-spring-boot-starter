package com.github.tocrhz.mqtt.autoconfigure;

import com.github.tocrhz.mqtt.annotation.MqttSubscribe;
import com.github.tocrhz.mqtt.subscriber.MqttSubscriber;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.LinkedList;

/**
 * When Bean is initialized, filter out the methods annotated with @MqttSubscribe, and create MqttSubscriber
 *
 * @author tocrhz
 * @see MqttSubscribe
 * @see MqttSubscriber
 */
@Component
public class MqttSubscribeProcessor implements BeanPostProcessor {

    @Value("${mqtt.disable:false}")
    private Boolean disable;

    @Autowired
    MqttConnector mqttConnector;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (disable == null || !disable) {
            Method[] methods = bean.getClass().getMethods();
            for (Method method : methods) {
                if (method.isAnnotationPresent(MqttSubscribe.class)) {
                    mqttConnector.subscribe(MqttSubscriber.of(bean, method));
                }
            }
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
}
