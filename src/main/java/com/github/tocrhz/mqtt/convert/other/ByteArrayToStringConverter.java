package com.github.tocrhz.mqtt.convert.other;

import org.springframework.core.convert.converter.Converter;

/**
 * byte[] => string
 */
public interface ByteArrayToStringConverter extends Converter<byte[], String> {
}
