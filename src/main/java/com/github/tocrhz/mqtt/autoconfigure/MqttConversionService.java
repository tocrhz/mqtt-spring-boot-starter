package com.github.tocrhz.mqtt.autoconfigure;

import com.github.tocrhz.mqtt.convert.*;
import com.github.tocrhz.mqtt.convert.other.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.convert.support.GenericConversionService;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author tocrhz
 */
public class MqttConversionService extends GenericConversionService {
    private final static Logger log = LoggerFactory.getLogger(MqttConversionService.class);
    private static volatile MqttConversionService sharedInstance;

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

    public static void addBeans(ListableBeanFactory beanFactory) {
        MqttConversionService registry = MqttConversionService.getSharedInstance();
        // 其他默认
        registry.addConverter((StringToByteArrayConverter) source -> source.getBytes(StandardCharsets.UTF_8));
        registry.addConverter((ByteArrayToStringConverter) source -> new String(source, StandardCharsets.UTF_8));
        registry.addConverter((ByteArrayToBooleanConverter) source -> Boolean.parseBoolean(new String(source, StandardCharsets.UTF_8)));
        registry.addConverter((ByteArrayToByteConverter) source -> Byte.parseByte(new String(source, StandardCharsets.UTF_8)));
        registry.addConverter((ByteArrayToShortConverter) source -> Short.parseShort(new String(source, StandardCharsets.UTF_8)));
        registry.addConverter((ByteArrayToIntegerConverter) source -> Integer.parseInt(new String(source, StandardCharsets.UTF_8)));
        registry.addConverter((ByteArrayToLongConverter) source -> Long.parseLong(new String(source, StandardCharsets.UTF_8)));
        registry.addConverter((ByteArrayToFloatConverter) source -> Float.parseFloat(new String(source, StandardCharsets.UTF_8)));
        registry.addConverter((ByteArrayToDoubleConverter) source -> Double.parseDouble(new String(source, StandardCharsets.UTF_8)));
        // 默认转换类
        beanFactory.getBeansOfType(PayloadDeserialize.class).values().forEach(registry::addConverterFactory);
        beanFactory.getBeansOfType(PayloadSerialize.class).values().forEach(registry::addConverter);
        // 其他转换类
        beanFactory.getBeansOfType(ConverterFactory.class).values().forEach(registry::addConverterFactory);
        beanFactory.getBeansOfType(Converter.class).values().forEach(registry::addConverter);
        beanFactory.getBeansOfType(GenericConverter.class).values().forEach(registry::addConverter);
    }

    public byte[] toBytes(Object source) {
        if (source == null) {
            return null;
        }
        byte[] convert = toBytes(true, this, source);
        if (convert == null) {
            // 如果无法转换, 则使用Spring的转换方法
            convert = toBytes(false, ApplicationConversionService.getSharedInstance(), source);
        }
        return convert;
    }

    public Object fromBytes(byte[] source, Class<?> target, List<Converter<Object, Object>> converters) {
        if (source == null) {
            return null;
        }
        Object payload = source;
        if (converters != null && !converters.isEmpty()) {
            // 直接配置到注解里的转换方法优先级最高, 按顺序执行
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
        Object convert = fromBytes(true, this, source, target);
        if (convert == null) {
            // 如果无法转换, 则使用Spring的转换方法
            convert = fromBytes(false, ApplicationConversionService.getSharedInstance(), source, target);
        }
        return convert;
    }

    private static byte[] toBytes(boolean self, ConversionService service, Object source) {
        Class<?> src = source.getClass();
        try {
            // 如果本身就是byte[], 直接返回
            if (src == byte[].class) {
                return (byte[]) source;
            }
            // 如果能直接转byte[], 就直接转 (只有自己才先尝试直接转byte[], 其他的都是先转String）
            if (self && service.canConvert(src, byte[].class)) {
                return service.convert(source, byte[].class);
            }
            // 如果不能, 就先转String, 再转为byte
            if (service.canConvert(src, String.class)) {
                String temporary = service.convert(source, String.class);
                if (temporary == null) {
                    log.warn("convert from '{}' to 'java.lang.String' return null.", src.getName());
                    return null;
                }
                return temporary.getBytes(StandardCharsets.UTF_8);
            } else {
                log.warn("convert from '{}' to 'byte[]' return null.", src.getName());
                return null;
            }
        } catch (Exception e) {
            log.error("convert from '{}' to 'byte[]' return null.", src.getName(), e);
            return null;
        }
    }

    private static Object fromBytes(boolean self, ConversionService service, Object source, Class<?> target) {
        Class<?> src = source.getClass();
        try {
            if (target == src) {
                return source;
            }
            if (self && src == byte[].class && service.canConvert(src, target)) {
                return service.convert(source, target);
            }
            if (src != byte[].class && service.canConvert(src, target)) {
                return service.convert(source, target);
            }
            if (service.canConvert(src, target)) {
                return service.convert(source, target);
            }
            if (service.canConvert(src, String.class) && service.canConvert(String.class, target)) {
                return service.convert(service.convert(source, String.class), target);
            } else {
                log.warn("convert from {} to {}", src.getName(), target.getName());
                return null;
            }
        } catch (Exception e) {
            log.error("convert from {} to {}", src.getName(), target.getName(), e);
            return null;
        }
    }
}

