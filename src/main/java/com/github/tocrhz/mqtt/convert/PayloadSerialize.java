package com.github.tocrhz.mqtt.convert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;

/**
 * @author tocrhz
 */
public interface PayloadSerialize extends Converter<Object, byte[]> {
    Logger log = LoggerFactory.getLogger(PayloadSerialize.class);
}
