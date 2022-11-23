package com.github.tocrhz.mqtt.convert.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tocrhz.mqtt.convert.PayloadSerialize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

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
            if (source instanceof byte[]) {
                return (byte[]) source;
            }else if (source instanceof String){
                return ((String) source).getBytes(StandardCharsets.UTF_8);
            }
            return objectMapper.writeValueAsBytes(source);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.warn("Payload serialize error: {}", e.getMessage(), e);
        }
        return null;
    }
}
