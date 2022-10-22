package com.github.tocrhz.mqtt.convert.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tocrhz.mqtt.convert.PayloadSerialize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 默认的对象转json字符串配置
 */
public class JacksonPayloadSerialize implements PayloadSerialize {
    private final static Logger log = LoggerFactory.getLogger(JacksonPayloadDeserialize.class);

    private final ObjectMapper objectMapper;

    public JacksonPayloadSerialize(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public byte[] convert(Object source) {
        try {
            return objectMapper.writeValueAsBytes(source);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.warn("Payload serialize error: {}", e.getMessage(), e);
        }
        return null;
    }
}
