package com.github.tocrhz.mqtt.properties;

/**
 * 遗愿相关配置.
 */
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

    /**
     * 遗愿主题.
     */
    public String getTopic() {
        return topic;
    }

    /**
     * 遗愿消息内容.
     */
    public Boolean getRetained() {
        return retained;
    }

    /**
     * 遗愿消息QOS.
     */
    public Integer getQos() {
        return qos;
    }

    /**
     * 遗愿消息是否保留.
     */
    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public void setQos(Integer qos) {
        this.qos = qos;
    }

    public void setRetained(Boolean retained) {
        this.retained = retained;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }
}