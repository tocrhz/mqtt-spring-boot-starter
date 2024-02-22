package com.github.tocrhz.mqtt.convert.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;

/**
 * 默认的对象转json字符串配置
 */
public class JacksonStringSerialize implements Converter<Object, String> {
    private final static Logger log = LoggerFactory.getLogger(JacksonStringSerialize.class);

    private final ObjectMapper objectMapper;

    public JacksonStringSerialize(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 对象转json
     *
     * @param source the source object to convert, which must be an instance of {@code S} (never {@code null})
     * @return json
     */
    @Override
    public String convert(Object source) {
        try {
            if (source instanceof String) {
                return (String) source;
            }
            return objectMapper.writeValueAsString(source);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.warn("Payload serialize error: {}", e.getMessage(), e);
        }
        return null;
    }
}
