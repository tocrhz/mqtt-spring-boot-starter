package com.github.tocrhz.mqtt.convert;

import org.springframework.core.convert.converter.Converter;

/**
 * @author tocrhz
 */
public interface PayloadSerialize extends Converter<Object, byte[]> {
}
