package com.github.tocrhz.mqtt.properties;

import lombok.Getter;
import lombok.Setter;

/**
 * 遗愿相关配置.
 */
@Getter
@Setter
public class WillProperties {
    /**
     * 遗愿主题.
     */
    private String topic;
    /**
     * 遗愿消息内容.
     */
    private String payload;
    /**
     * 遗愿消息QOS.
     */
    private Integer qos;
    /**
     * 遗愿消息是否保留.
     */
    private Boolean retained;
}