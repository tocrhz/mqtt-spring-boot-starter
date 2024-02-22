package com.github.tocrhz.mqtt.convert.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tocrhz.mqtt.convert.PayloadDeserialize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 默认的json字符串转对象配置
 */
public class JacksonPayloadDeserialize implements PayloadDeserialize {
    private final static Logger log = LoggerFactory.getLogger(JacksonPayloadDeserialize.class);

    private final ObjectMapper objectMapper;

    public JacksonPayloadDeserialize(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 获取转换方法
     *
     * @param targetType the target type to convert to
     * @param <T>        目标类型
     * @return target type object
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> Converter<byte[], T> getConverter(Class<T> targetType) {
        return source -> {
            try {
                if (targetType == byte[].class) {
                    return (T) source;
                } else if (targetType == String.class) {
                    return (T) new String(source, StandardCharsets.UTF_8);
                }
                return objectMapper.readValue(source, targetType);
            } catch (IOException e) {
                log.warn("Payload deserialize error: {}", e.getMessage(), e);
            }
            return null;
        };
    }
}
