package com.github.tocrhz.mqtt.autoconfigure;

import com.github.tocrhz.mqtt.annotation.MqttSubscribe;
import com.github.tocrhz.mqtt.interceptor.DefaultPreInterceptor;
import com.github.tocrhz.mqtt.interceptor.PreInterceptor;
import com.github.tocrhz.mqtt.subscriber.MqttSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

/**
 * When Bean is initialized, filter out the methods annotated with @MqttSubscribe, and create MqttSubscriber
 *
 * @author tocrhz
 * @see MqttSubscribe
 * @see MqttSubscriber
 */
@Component
public class MqttSubscribeProcessor implements BeanPostProcessor, ApplicationContextAware {

    private final static Logger log = LoggerFactory.getLogger(MqttSubscribeProcessor.class);

    // subscriber cache
    public static final LinkedList<MqttSubscriber> SUBSCRIBERS = new LinkedList<>();

    private ApplicationContext applicationContext;

    @Value("${mqtt.disable:false}")
    private Boolean disable;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (disable == null || !disable) {
            Method[] methods = bean.getClass().getMethods();
            // 获取所有的前置拦截器
            Collection<PreInterceptor> preInterceptors = applicationContext.getBeansOfType(PreInterceptor.class).values();
            for (Method method : methods) {
                if (method.isAnnotationPresent(MqttSubscribe.class)) {
                    MqttSubscriber of = MqttSubscriber.of(bean, method);
                    of.addPreInterceptors(preInterceptors);
                    SUBSCRIBERS.add(of);
                }
            }
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public void setApplicationContext(ApplicationContext ac) throws BeansException {
        this.applicationContext = ac;
    }

}
