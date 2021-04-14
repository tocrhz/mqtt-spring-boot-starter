package com.github.tocrhz.mqtt.properties;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

/**
 * MQTT连接配置
 */
public class ConnectionProperties {

    /**
     * MQTT服务器地址, 必填, 可以配置多个.
     *
     * @see MqttConnectOptions#setServerURIs(String[])
     */
    private String[] uri = new String[]{"tcp://127.0.0.1:1883"};

    /**
     * 用户名.
     *
     * @see MqttConnectOptions#setUserName(String)
     */
    private String username;

    /**
     * 密码.
     *
     * @see MqttConnectOptions#setPassword(char[])
     */
    private String password;

    /**
     * 是否启用共享订阅,对于不同的Broker,共享订阅可能无效(EMQ已测可用).
     */
    private Boolean enableSharedSubscription;

    /**
     * 发布消息默认使用的QOS, 默认 0.
     */
    private Integer defaultPublishQos;

    /**
     * 最大重连等待时间(秒).
     *
     * @see MqttConnectOptions#setMaxReconnectDelay(int)
     */
    private Integer maxReconnectDelay;

    /**
     * KeepAlive 周期(秒).
     *
     * @see MqttConnectOptions#setKeepAliveInterval(int)
     */
    private Integer keepAliveInterval;

    /**
     * 连接超时时间(秒).
     *
     * @see MqttConnectOptions#setConnectionTimeout(int)
     */
    private Integer connectionTimeout;

    /**
     * 发送超时时间(秒).
     *
     * @see MqttConnectOptions#setExecutorServiceTimeout(int)
     */
    private Integer executorServiceTimeout;

    /**
     * 是否清除会话.
     *
     * @see MqttConnectOptions#setCleanSession(boolean)
     */
    private Boolean cleanSession;

    /**
     * 断开是否重新连接.
     *
     * @see MqttConnectOptions#setAutomaticReconnect(boolean)
     */
    private Boolean automaticReconnect;

    /**
     * 遗愿相关配置.
     */
    private WillProperties will;

    /**
     * 最大重连等待时间(秒).
     *
     * @return Integer
     * @see MqttConnectOptions#setMaxReconnectDelay(int)
     */
    public Integer getMaxReconnectDelay() {
        return maxReconnectDelay;
    }

    /**
     * KeepAlive 周期(秒).
     *
     * @return Integer
     * @see MqttConnectOptions#setKeepAliveInterval(int)
     */
    public Integer getKeepAliveInterval() {
        return keepAliveInterval;
    }

    /**
     * 发送超时时间(秒).
     *
     * @return Integer
     * @see MqttConnectOptions#setExecutorServiceTimeout(int)
     */
    public Integer getExecutorServiceTimeout() {
        return executorServiceTimeout;
    }

    /**
     * 连接超时时间(秒).
     *
     * @return Integer
     * @see MqttConnectOptions#setConnectionTimeout(int)
     */
    public Integer getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * 是否清除会话.
     *
     * @return Boolean
     * @see MqttConnectOptions#setCleanSession(boolean)
     */
    public Boolean getCleanSession() {
        return cleanSession;
    }

    /**
     * 断开是否重新连接.
     *
     * @return Boolean
     * @see MqttConnectOptions#setAutomaticReconnect(boolean)
     */
    public Boolean getAutomaticReconnect() {
        return automaticReconnect;
    }

    /**
     * 用户名.
     *
     * @return String
     * @see MqttConnectOptions#setUserName(String)
     */
    public String getUsername() {
        return username;
    }

    /**
     * 遗愿相关配置.
     *
     * @return WillProperties
     */
    public WillProperties getWill() {
        return will;
    }

    /**
     * 密码.
     *
     * @return password
     * @see MqttConnectOptions#setPassword(char[])
     */
    public String getPassword() {
        return password;
    }

    /**
     * MQTT服务器地址, 必填, 可以配置多个.
     *
     * @return String[]
     * @see MqttConnectOptions#setServerURIs(String[])
     */
    public String[] getUri() {
        return uri;
    }

    /**
     * 是否启用共享订阅,对于不同的Broker,共享订阅可能无效(EMQ已测可用).
     *
     * @return Boolean
     */
    public Boolean getEnableSharedSubscription() {
        return enableSharedSubscription;
    }

    /**
     * 发布消息默认使用的QOS, 默认 0.
     * @return Integer
     */
    public Integer getDefaultPublishQos() {
        return defaultPublishQos;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setWill(WillProperties will) {
        this.will = will;
    }

    public void setAutomaticReconnect(Boolean automaticReconnect) {
        this.automaticReconnect = automaticReconnect;
    }

    public void setCleanSession(Boolean cleanSession) {
        this.cleanSession = cleanSession;
    }

    public void setConnectionTimeout(Integer connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public void setExecutorServiceTimeout(Integer executorServiceTimeout) {
        this.executorServiceTimeout = executorServiceTimeout;
    }

    public void setKeepAliveInterval(Integer keepAliveInterval) {
        this.keepAliveInterval = keepAliveInterval;
    }

    public void setMaxReconnectDelay(Integer maxReconnectDelay) {
        this.maxReconnectDelay = maxReconnectDelay;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setUri(String[] uri) {
        this.uri = uri;
    }

    public void setEnableSharedSubscription(Boolean enableSharedSubscription) {
        this.enableSharedSubscription = enableSharedSubscription;
    }

    public void setDefaultPublishQos(Integer defaultPublishQos) {
        this.defaultPublishQos = defaultPublishQos;
    }
}
