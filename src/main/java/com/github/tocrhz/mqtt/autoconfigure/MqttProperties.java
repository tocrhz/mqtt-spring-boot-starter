package com.github.tocrhz.mqtt.autoconfigure;

import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;

/**
 * MQTT properties.
 *
 * @author tocrhz
 * @see MqttConnectOptions
 */
@ConfigurationProperties(prefix = "mqtt")
public class MqttProperties {

    public MqttConnectOptions toOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setMaxReconnectDelay(maxReconnectDelay);
        options.setKeepAliveInterval(keepAliveInterval);
        options.setConnectionTimeout(connectionTimeout);
        options.setCleanSession(cleanSession);
        options.setAutomaticReconnect(automaticReconnect);
        options.setExecutorServiceTimeout(executorServiceTimeout);
        options.setServerURIs(uri);
        if (username != null && password != null) {
            this.username = username.trim();
            this.password = password.trim();
            if (username.length() > 0 && password.length() > 0) {
                options.setUserName(username);
                options.setPassword(password.toCharArray());
            }
        }
        if (!isBlank(will.topic) && !isBlank(will.payload)) {
            options.setWill(will.topic, will.payload.getBytes(StandardCharsets.UTF_8), will.qos, will.retained);
        }
        return options;
    }

    /**
     * 是否禁用
     */
    private Boolean disable = false;

    /**
     * MQTT服务器地址, 可以配置多个.
     *
     * @see MqttConnectOptions#setServerURIs(String[])
     */
    private String[] uri = new String[]{"tcp://127.0.0.1:1883"};

    /**
     * 客户端ID, 默认为MqttAsyncClient.generateClientId().
     */
    private String clientId;

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
     * 最大重连等待时间.
     *
     * @see MqttConnectOptions#setMaxReconnectDelay(int)
     */
    private int maxReconnectDelay = 30;

    /**
     * KeepAlive 周期.
     *
     * @see MqttConnectOptions#setKeepAliveInterval(int)
     */
    private int keepAliveInterval = 30;

    /**
     * 连接超时时间.
     *
     * @see MqttConnectOptions#setConnectionTimeout(int)
     */
    private int connectionTimeout = 30;

    /**
     * 发送超时时间.
     *
     * @see MqttConnectOptions#setExecutorServiceTimeout(int)
     */
    private int executorServiceTimeout = 10;

    /**
     * 是否清除会话.
     *
     * @see MqttConnectOptions#setCleanSession(boolean)
     */
    private boolean cleanSession = true;

    /**
     * 断开是否重新连接.
     *
     * @see MqttConnectOptions#setAutomaticReconnect(boolean)
     */
    private boolean automaticReconnect = true;

    public Boolean getDisable() {
        return disable;
    }

    public void setDisable(Boolean disable) {
        this.disable = disable;
    }

    public String[] getUri() {
        return uri;
    }

    public void setUri(String[] uri) {
        this.uri = uri;
    }

    public String getClientId() {
        if (isBlank(clientId)) {
            clientId = MqttAsyncClient.generateClientId();
        }
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getMaxReconnectDelay() {
        return maxReconnectDelay;
    }

    public void setMaxReconnectDelay(int maxReconnectDelay) {
        this.maxReconnectDelay = maxReconnectDelay;
    }

    public int getKeepAliveInterval() {
        return keepAliveInterval;
    }

    public void setKeepAliveInterval(int keepAliveInterval) {
        this.keepAliveInterval = keepAliveInterval;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getExecutorServiceTimeout() {
        return executorServiceTimeout;
    }

    public void setExecutorServiceTimeout(int executorServiceTimeout) {
        this.executorServiceTimeout = executorServiceTimeout;
    }

    public boolean isCleanSession() {
        return cleanSession;
    }

    public void setCleanSession(boolean cleanSession) {
        this.cleanSession = cleanSession;
    }

    public boolean isAutomaticReconnect() {
        return automaticReconnect;
    }

    public void setAutomaticReconnect(boolean automaticReconnect) {
        this.automaticReconnect = automaticReconnect;
    }

    /**
     * 遗愿相关配置.
     */
    private WillProperties will = new WillProperties();

    public WillProperties getWill() {
        return will;
    }

    public void setWill(WillProperties will) {
        this.will = will;
    }

    /**
     * 遗愿相关配置.
     */
    public static class WillProperties {
        /**
         * 遗愿主题.
         */
        private String topic;
        /**
         * 遗愿消息内容.
         */
        private String payload = "default will msg.";
        /**
         * 遗愿消息QOS.
         */
        private int qos = 0;
        /**
         * 遗愿消息是否保留.
         */
        private boolean retained = false;

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public String getPayload() {
            return payload;
        }

        public void setPayload(String payload) {
            this.payload = payload;
        }

        public int getQos() {
            return qos;
        }

        public void setQos(int qos) {
            this.qos = qos;
        }

        public boolean isRetained() {
            return retained;
        }

        public void setRetained(boolean retained) {
            this.retained = retained;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
