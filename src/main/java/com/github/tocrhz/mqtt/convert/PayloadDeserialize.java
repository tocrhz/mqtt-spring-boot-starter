package com.github.tocrhz.mqtt.convert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.ConverterFactory;

/**
 * @author tocrhz
 */
public interface PayloadDeserialize extends ConverterFactory<byte[], Object> {
    Logger log = LoggerFactory.getLogger(PayloadDeserialize.class);
}
