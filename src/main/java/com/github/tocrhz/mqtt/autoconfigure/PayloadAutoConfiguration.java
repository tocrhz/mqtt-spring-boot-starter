package com.github.tocrhz.mqtt.autoconfigure;


import org.springframework.core.convert.converter.ConverterRegistry;

/**
 * @author tocrhz
 */
public abstract class PayloadAutoConfiguration {

    public PayloadAutoConfiguration() {
        this.registry(MqttConversionService.getSharedInstance());
    }

    protected abstract void registry(ConverterRegistry registry);
}
