package com.github.tocrhz.mqtt.autoconfigure;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.util.VersionUtil;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.github.tocrhz.mqtt.convert.PayloadDeserialize;
import com.github.tocrhz.mqtt.convert.PayloadSerialize;
import com.github.tocrhz.mqtt.convert.jackson.JacksonPayloadDeserialize;
import com.github.tocrhz.mqtt.convert.jackson.JacksonPayloadSerialize;
import com.github.tocrhz.mqtt.convert.jackson.JacksonStringDeserialize;
import com.github.tocrhz.mqtt.convert.jackson.JacksonStringSerialize;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;

/**
 * default mqtt payload config for jackson.
 *
 * @author tocrhz
 */
@Order
@AutoConfigureAfter({JacksonAutoConfiguration.class})
@ConditionalOnClass(ObjectMapper.class)
@Configuration
public class PayloadJacksonAutoConfiguration {

    public PayloadJacksonAutoConfiguration(ListableBeanFactory beanFactory) {
        MqttConversionService registry = MqttConversionService.getSharedInstance();

        ObjectMapper objectMapper = objectMapper();
        // 默认转换类
        Map<String, PayloadDeserialize> deserializeMap = beanFactory.getBeansOfType(PayloadDeserialize.class);
        if (deserializeMap.isEmpty()) {
            registry.addConverterFactory(jacksonPayloadDeserialize(objectMapper));
            registry.addConverterFactory(jacksonStringDeserialize(objectMapper));
        } else {
            deserializeMap.values().forEach(registry::addConverterFactory);
        }
        Map<String, PayloadSerialize> serializeMap = beanFactory.getBeansOfType(PayloadSerialize.class);
        if (serializeMap.isEmpty()) {
            registry.addConverter(jacksonPayloadSerialize(objectMapper));
            registry.addConverter(jacksonStringSerialize(objectMapper));
        } else {
            serializeMap.values().forEach(registry::addConverter);
        }
    }

    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        objectMapper.registerModule(new MqttDefaultJacksonModule());
        return objectMapper;
    }

    public JacksonPayloadSerialize jacksonPayloadSerialize(ObjectMapper objectMapper) {
        return new JacksonPayloadSerialize(objectMapper);
    }

    public JacksonPayloadDeserialize jacksonPayloadDeserialize(ObjectMapper objectMapper) {
        return new JacksonPayloadDeserialize(objectMapper);
    }

    public JacksonStringSerialize jacksonStringSerialize(ObjectMapper objectMapper) {
        return new JacksonStringSerialize(objectMapper);
    }

    public JacksonStringDeserialize jacksonStringDeserialize(ObjectMapper objectMapper) {
        return new JacksonStringDeserialize(objectMapper);
    }

    public static class MqttDefaultJacksonModule extends SimpleModule {
        public static final Version VERSION = VersionUtil.parseVersion("1.2.8",
                "com.github.tocrhz",
                "mqtt-spring-boot-starter");

        private final static ZoneId ZONE_ID = ZoneId.of("GMT+8");
        private final static DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        private final static DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        private final static DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

        public MqttDefaultJacksonModule() {
            super(VERSION);

            addSerializer(LocalDateTime.class, LOCAL_DATE_TIME_JSON_SERIALIZER);
            addSerializer(LocalDate.class, LOCAL_DATE_JSON_SERIALIZER);
            addSerializer(LocalTime.class, LOCAL_TIME_JSON_SERIALIZER);
            addSerializer(Date.class, DATE_JSON_SERIALIZER);

            addDeserializer(LocalDateTime.class, LOCAL_DATE_TIME_JSON_DESERIALIZER);
            addDeserializer(LocalDate.class, LOCAL_DATE_JSON_DESERIALIZER);
            addDeserializer(LocalTime.class, LOCAL_TIME_JSON_DESERIALIZER);
            addDeserializer(Date.class, DATE_JSON_DESERIALIZER);
        }

        private final static JsonSerializer<LocalDateTime> LOCAL_DATE_TIME_JSON_SERIALIZER = new JsonSerializer<LocalDateTime>() {
            @Override
            public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                if (value == null) {
                    gen.writeNull();
                } else {
                    gen.writeString(value.atZone(ZONE_ID).format(DATE_TIME_FORMATTER));
                }
            }
        };
        private final static JsonSerializer<LocalDate> LOCAL_DATE_JSON_SERIALIZER = new JsonSerializer<LocalDate>() {

            @Override
            public void serialize(LocalDate value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                if (value == null) {
                    gen.writeNull();
                } else {
                    gen.writeString(value.format(DATE_FORMATTER));
                }
            }
        };
        private final static JsonSerializer<LocalTime> LOCAL_TIME_JSON_SERIALIZER = new JsonSerializer<LocalTime>() {

            @Override
            public void serialize(LocalTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                if (value == null) {
                    gen.writeNull();
                } else {
                    gen.writeString(value.format(TIME_FORMATTER));
                }
            }
        };
        private final static JsonSerializer<Date> DATE_JSON_SERIALIZER = new JsonSerializer<Date>() {

            @Override
            public void serialize(Date value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                if (value == null) {
                    gen.writeNull();
                } else {
                    gen.writeString(DATE_TIME_FORMATTER.format(value.toInstant().atZone(ZONE_ID)));
                }
            }
        };

        private final static JsonDeserializer<LocalDateTime> LOCAL_DATE_TIME_JSON_DESERIALIZER = new JsonDeserializer<LocalDateTime>() {
            @Override
            public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                String value = p.getValueAsString();
                if (StringUtils.hasText(value)) {
                    return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
                }
                return null;
            }
        };
        private final static JsonDeserializer<LocalDate> LOCAL_DATE_JSON_DESERIALIZER = new JsonDeserializer<LocalDate>() {
            @Override
            public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                String value = p.getValueAsString();
                if (StringUtils.hasText(value)) {
                    return LocalDate.parse(value, DATE_FORMATTER);
                }
                return null;
            }
        };
        private final static JsonDeserializer<LocalTime> LOCAL_TIME_JSON_DESERIALIZER = new JsonDeserializer<LocalTime>() {
            @Override
            public LocalTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                String value = p.getValueAsString();
                if (StringUtils.hasText(value)) {
                    return LocalTime.parse(value, TIME_FORMATTER);
                }
                return null;
            }
        };
        private final static JsonDeserializer<Date> DATE_JSON_DESERIALIZER = new JsonDeserializer<Date>() {
            @Override
            public Date deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                String value = p.getValueAsString();
                if (StringUtils.hasText(value)) {
                    return Date.from(LocalDateTime.parse(value, DATE_TIME_FORMATTER).atZone(ZONE_ID).toInstant());
                }
                return null;
            }
        };

    }
}