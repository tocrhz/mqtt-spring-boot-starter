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
    int[] qos() default 0;

    /**
     * clientIds
     *
     * @return clientId, default all client
     */
    String[] clients() default {};

    /**
     * Shared subscription, default false;
     * <p>
     * for topic one-to-one.
     * <p>
     * If not one-to-one. fill by last shared and ignore the superfluous.
     *
     * @return true or false
     * @see #groups()
     */
    boolean[] shared() default false;

    /**
     * Shared subscription group,
     * <p>
     * if default, use '$queue/&lt;topic&gt;'
     * <p>
     * if group not blank, use '$share/&lt;group&gt;/&lt;topic&gt;'
     * for topic one-to-one.
     * <p>
     * If not one-to-one. fill by last groups and ignore the superfluous.
     *
     * @return String[] groups
     */
    String[] groups() default "";
}
