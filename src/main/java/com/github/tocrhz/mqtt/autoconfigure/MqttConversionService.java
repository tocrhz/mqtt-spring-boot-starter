package com.github.tocrhz.mqtt.autoconfigure;

import com.github.tocrhz.mqtt.convert.PayloadDeserialize;
import com.github.tocrhz.mqtt.convert.PayloadSerialize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author tocrhz
 */
public class MqttConversionService extends GenericConversionService {
    private final static Logger log = LoggerFactory.getLogger(MqttConversionService.class);
    private static volatile MqttConversionService sharedInstance;

    public MqttConversionService() {
        configure(this);
    }

    public static MqttConversionService getSharedInstance() {
        if (sharedInstance == null) {
            synchronized (MqttConversionService.class) {
                if (sharedInstance == null) {
                    sharedInstance = new MqttConversionService();
                }
            }
        }
        return sharedInstance;
    }

    public static void configure(ConverterRegistry registry) {
        DefaultConversionService.addDefaultConverters(registry);
        registry.addConverter((StringToByteArrayConverter) source -> source.getBytes(StandardCharsets.UTF_8));
        registry.addConverter((ByteArrayToStringConverter) source -> new String(source, StandardCharsets.UTF_8));
        registry.addConverter((ByteArrayToBooleanConverter) source -> Boolean.parseBoolean(new String(source, StandardCharsets.UTF_8)));
        registry.addConverter((ByteArrayToByteConverter) source -> Byte.parseByte(new String(source, StandardCharsets.UTF_8)));
        registry.addConverter((ByteArrayToShortConverter) source -> Short.parseShort(new String(source, StandardCharsets.UTF_8)));
        registry.addConverter((ByteArrayToIntegerConverter) source -> Integer.parseInt(new String(source, StandardCharsets.UTF_8)));
        registry.addConverter((ByteArrayToLongConverter) source -> Long.parseLong(new String(source, StandardCharsets.UTF_8)));
        registry.addConverter((ByteArrayToFloatConverter) source -> Float.parseFloat(new String(source, StandardCharsets.UTF_8)));
        registry.addConverter((ByteArrayToDoubleConverter) source -> Double.parseDouble(new String(source, StandardCharsets.UTF_8)));
    }

    public static void addBeans(ConverterRegistry registry, ListableBeanFactory beanFactory) {
        Set<Object> beans = new LinkedHashSet<>();
        beans.addAll(beanFactory.getBeansOfType(PayloadDeserialize.class).values());
        beans.addAll(beanFactory.getBeansOfType(PayloadSerialize.class).values());
        beans.addAll(beanFactory.getBeansOfType(ConverterFactory.class).values());
        beans.addAll(beanFactory.getBeansOfType(GenericConverter.class).values());
        beans.addAll(beanFactory.getBeansOfType(Converter.class).values());
        for (Object bean : beans) {
            if (bean instanceof PayloadDeserialize) {
                registry.addConverterFactory((PayloadDeserialize) bean);
            } else if (bean instanceof ConverterFactory) {
                registry.addConverterFactory((ConverterFactory<?, ?>) bean);
            } else if (bean instanceof PayloadSerialize) {
                registry.addConverter((PayloadSerialize) bean);
            } else if (bean instanceof GenericConverter) {
                registry.addConverter((GenericConverter) bean);
            } else if (bean instanceof Converter) {
                registry.addConverter((Converter<?, ?>) bean);
            }
        }
    }

    public byte[] toBytes(Object source) {
        if (source == null) {
            return null;
        }
        Class<?> src = source.getClass();
        if (src == byte[].class) {
            return (byte[]) source;
        } else if (canConvert(src, byte[].class)) {
            return convert(source, byte[].class);
        } else if (canConvert(src, String.class)) {
            String temporary = convert(source, String.class);
            if (temporary == null) {
                log.warn("Execute covert from {} to {} return null.", src.getName(), byte[].class);
                return null;
            }
            return temporary.getBytes(StandardCharsets.UTF_8);
        } else {
            log.warn("Unsupported covert from {} to {}", src.getName(), byte[].class);
            return null;
        }
    }

    public Object fromBytes(byte[] source, Class<?> target, List<Converter<Object, Object>> converters) {
        if (source == null) {
            return null;
        }
        Object payload = source;
        if (converters != null && !converters.isEmpty()) {
            for (Converter<Object, Object> converter : converters) {
                try {
                    if (payload == null) {
                        log.warn("Execute covert {} return null.", converter.getClass().getName());
                        return null;
                    } else {
                        payload = converter.convert(payload);
                    }
                } catch (Exception e) {
                    log.error("Execute covert {} failed.", converter.getClass().getName(), e);
                    return null;
                }
            }
        }
        if (payload == null) {
            return null;
        }
        Class<?> src = payload.getClass();
        if (target == src) {
            return source;
        } else {
            if (canConvert(src, target)) {
                return convert(payload, target);
            } else if (canConvert(String.class, target)) {
                return convert(convert(payload, String.class), target);
            } else {
                log.warn("Unsupported covert from {} to {}", src.getName(), target.getName());
                return null;
            }
        }
    }

    interface StringToByteArrayConverter extends Converter<String, byte[]> {
    }

    interface ByteArrayToStringConverter extends Converter<byte[], String> {
    }

    interface ByteArrayToBooleanConverter extends Converter<byte[], Boolean> {
    }

    interface ByteArrayToByteConverter extends Converter<byte[], Byte> {
    }

    interface ByteArrayToShortConverter extends Converter<byte[], Short> {
    }

    interface ByteArrayToIntegerConverter extends Converter<byte[], Integer> {
    }

    interface ByteArrayToLongConverter extends Converter<byte[], Long> {
    }

    interface ByteArrayToFloatConverter extends Converter<byte[], Float> {
    }

    interface ByteArrayToDoubleConverter extends Converter<byte[], Double> {
    }
}
