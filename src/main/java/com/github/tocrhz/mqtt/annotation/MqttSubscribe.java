package com.github.tocrhz.mqtt.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Subscribe topics.
 *
 * @author tocrhz
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface MqttSubscribe {
    /**
     * topics
     *
     * @return topics
     */
    String[] value();

    /**
     * QOS for topic one-to-one.
     * <p>
     * If not one-to-one. fill by last qos and ignore the superfluous.
     *
     * @return QOSs
     */
    int[] qos() default 1;

    /**
     * clientIds
     *
     * @return clientId, default all client
     */
    String[] clients() default {};

    /**
     * Shared subscription, default false;
     * @see #groups()
     * @return false/true
     */
    boolean[] shared() default false;

    /**
     * Shared subscription group, default: '$queue/&lt;topic&gt;'
     * <p>
     * '$share/&lt;group&gt;/&lt;topic&gt;' if group not blank
     * @return Shared subscription
     */
    String[] groups() default "";
}
