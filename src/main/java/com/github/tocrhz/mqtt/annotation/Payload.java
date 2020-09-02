package com.github.tocrhz.mqtt.annotation;

import org.springframework.core.convert.converter.Converter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Message content.
 * <p>
 * If there is no such annotation in the parameter list, the default custom type has this annotation.
 * <p>
 * If there is this annotation in the parameter list, only the message content will be assigned to the annotated parameter.
 *
 * @author tocrhz
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface Payload {

    /**
     * The processing before conversion is executed sequentially, starting from byte[] and ending with the target type.
     * <p>
     * If the result is the same as the target type after the execution in sequence, it is directly assigned,
     * if it is different, MqttConversionService is called for conversion.
     *
     * @return Converter
     */
    Class<? extends Converter<?, ?>>[] value() default {};
}
