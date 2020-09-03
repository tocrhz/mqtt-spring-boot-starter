package com.github.tocrhz.mqtt.convert;

import org.springframework.core.convert.converter.ConverterFactory;

/**
 * @author tocrhz
 */
public interface PayloadDeserialize extends ConverterFactory<byte[], Object> {
}
