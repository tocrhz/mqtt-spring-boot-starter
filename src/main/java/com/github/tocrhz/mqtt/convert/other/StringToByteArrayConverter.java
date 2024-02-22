package com.github.tocrhz.mqtt.convert.other;

import org.springframework.core.convert.converter.Converter;

/**
 * string => byte[]
 */
public interface StringToByteArrayConverter extends Converter<String, byte[]> {
}
