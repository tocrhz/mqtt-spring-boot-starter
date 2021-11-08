package com.github.tocrhz.mqtt.subscriber;

import com.github.tocrhz.mqtt.annotation.NamedValue;
import com.github.tocrhz.mqtt.annotation.Payload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.LinkedList;

/**
 * @author tocrhz
 */
final class ParameterModel {
    private final static Logger log = LoggerFactory.getLogger(ParameterModel.class);

    private boolean sign; // 标记为消息内容, 若参数为String类型, 并且无标记, 则赋值topic.
    private boolean required;
    private Class<?> type;
    private String name;
    private Object defaultValue;
    private LinkedList<Converter<Object, Object>> converters;

    private ParameterModel() {
    }

    public static LinkedList<ParameterModel> of(Method method) {
        LinkedList<ParameterModel> parameters = new LinkedList<>();
        Class<?>[] parameterTypes = method.getParameterTypes();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        for (int i = 0; i < parameterTypes.length; i++) {
            ParameterModel model = new ParameterModel();
            parameters.add(model);
            model.type = parameterTypes[i];
            model.defaultValue = defaultValue(model.type);
            Annotation[] annotations = parameterAnnotations[i];
            if (annotations != null && annotations.length > 0) {
                for (Annotation annotation : annotations) {
                    if (annotation.annotationType() == NamedValue.class) {
                        NamedValue namedValue = (NamedValue) annotation;
                        model.required = model.required || namedValue.required();
                        model.name = namedValue.value();
                    }
                    if (annotation.annotationType() == Payload.class) {
                        Payload payload = (Payload) annotation;
                        model.sign = true;
                        model.required = model.required || payload.required();
                        model.converters = toConverters(payload.value());
                    }
//                    if (annotation.annotationType() == NonNull.class) {
//                        model.required = true;
//                    }
                }
            }
        }
        return parameters;
    }

    @SuppressWarnings("unchecked")
    public static LinkedList<Converter<Object, Object>> toConverters(Class<? extends Converter<?, ?>>[] classes) {
        if (classes == null || classes.length == 0) {
            return null;
        } else {
            LinkedList<Converter<Object, Object>> converters = new LinkedList<>();
            for (Class<? extends Converter<?, ?>> covert : classes) {
                try {
                    converters.add((Converter<Object, Object>) covert.getDeclaredConstructor().newInstance());
                } catch (Exception e) {
                    log.error("Create converter instance failed.", e);
                }
            }
            return converters;
        }
    }

    public boolean isSign() {
        return sign;
    }

    public boolean isRequired() {
        return required;
    }

    public Class<?> getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public LinkedList<Converter<Object, Object>> getConverters() {
        return converters;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    private static Object defaultValue(Class<?> type) {
        if (type.isPrimitive()) {
            if (type == boolean.class) {
                return false;
            }
            if (type == char.class) {
                return (char) 0;
            }
            if (type == byte.class) {
                return (byte) 0;
            }
            if (type == short.class) {
                return (short) 0;
            }
            if (type == int.class) {
                return 0;
            }
            if (type == long.class) {
                return 0L;
            }
            if (type == float.class) {
                return 0.0f;
            }
            if (type == double.class) {
                return 0.0d;
            }
        }
        return null;
    }
}
