package com.github.tocrhz.mqtt.properties;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

/**
 * MQTT连接配置
 */
@Setter
@Getter
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
    @Getter
    private WillProperties will;
}
