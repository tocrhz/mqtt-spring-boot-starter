package com.github.tocrhz.mqtt.autoconfigure;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * For multiple client support.
 *
 * @author tocrhz
 */
@Slf4j
public class MqttClientWrapper implements IMqttAsyncClient {
    private String clientId = null; // see initialization

    protected MqttAsyncClient newMqttClient(String clientId, String[] uri) throws MqttException {
        return new MqttAsyncClient(uri[0], clientId, new MemoryPersistence());
    }

    public MqttAsyncClient getInstance() {
        return MQTT_CLIENT_MAP.get(clientId);
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public IMqttToken connect() throws MqttException {
        return getInstance().connect();
    }

    @Override
    public IMqttToken connect(MqttConnectOptions options) throws MqttException {
        return getInstance().connect(options);
    }

    @Override
    public IMqttToken connect(Object userContext, IMqttActionListener callback) throws MqttException {
        return getInstance().connect(userContext, callback);
    }

    @Override
    public IMqttToken connect(MqttConnectOptions options, Object userContext, IMqttActionListener callback) throws MqttException {
        return getInstance().connect(options, userContext, callback);
    }

    @Override
    public IMqttToken disconnect() throws MqttException {
        return getInstance().disconnect();
    }

    @Override
    public IMqttToken disconnect(long quiesceTimeout) throws MqttException {
        return getInstance().disconnect(quiesceTimeout);
    }

    @Override
    public IMqttToken disconnect(Object userContext, IMqttActionListener callback) throws MqttException {
        return getInstance().disconnect(userContext, callback);
    }

    @Override
    public IMqttToken disconnect(long quiesceTimeout, Object userContext, IMqttActionListener callback) throws MqttException {
        return getInstance().disconnect(quiesceTimeout, userContext, callback);
    }

    @Override
    public void disconnectForcibly() throws MqttException {
        getInstance().disconnectForcibly();
    }

    @Override
    public void disconnectForcibly(long disconnectTimeout) throws MqttException {
        getInstance().disconnectForcibly(disconnectTimeout);
    }

    @Override
    public void disconnectForcibly(long quiesceTimeout, long disconnectTimeout) throws MqttException {
        getInstance().disconnectForcibly(quiesceTimeout, disconnectTimeout);
    }

    @Override
    public boolean isConnected() {
        return getInstance().isConnected();
    }

    @Override
    public String getClientId() {
        return getInstance().getClientId();
    }

    @Override
    public String getServerURI() {
        return getInstance().getServerURI();
    }

    @Override
    public IMqttDeliveryToken publish(String topic, byte[] payload, int qos, boolean retained) throws MqttException {
        return getInstance().publish(topic, payload, qos, retained);
    }

    @Override
    public IMqttDeliveryToken publish(String topic, byte[] payload, int qos, boolean retained, Object userContext, IMqttActionListener callback) throws MqttException {
        return getInstance().publish(topic, payload, qos, retained, userContext, callback);
    }

    @Override
    public IMqttDeliveryToken publish(String topic, MqttMessage message) throws MqttException {
        return getInstance().publish(topic, message);
    }

    @Override
    public IMqttDeliveryToken publish(String topic, MqttMessage message, Object userContext, IMqttActionListener callback) throws MqttException {
        return getInstance().publish(topic, message, userContext, callback);
    }

    @Override
    public IMqttToken subscribe(String topicFilter, int qos) throws MqttException {
        return getInstance().subscribe(topicFilter, qos);
    }

    @Override
    public IMqttToken subscribe(String topicFilter, int qos, Object userContext, IMqttActionListener callback) throws MqttException {
        return getInstance().subscribe(topicFilter, qos, userContext, callback);
    }

    @Override
    public IMqttToken subscribe(String[] topicFilters, int[] qos) throws MqttException {
        return getInstance().subscribe(topicFilters, qos);
    }

    @Override
    public IMqttToken subscribe(String[] topicFilters, int[] qos, Object userContext, IMqttActionListener callback) throws MqttException {
        return getInstance().subscribe(topicFilters, qos, userContext, callback);
    }

    @Override
    public IMqttToken subscribe(String topicFilter, int qos, Object userContext, IMqttActionListener callback, IMqttMessageListener messageListener) throws MqttException {
        return getInstance().subscribe(topicFilter, qos, userContext, callback, messageListener);
    }

    @Override
    public IMqttToken subscribe(String topicFilter, int qos, IMqttMessageListener messageListener) throws MqttException {
        return getInstance().subscribe(topicFilter, qos, messageListener);
    }

    @Override
    public IMqttToken subscribe(String[] topicFilters, int[] qos, IMqttMessageListener[] messageListeners) throws MqttException {
        return getInstance().subscribe(topicFilters, qos, messageListeners);
    }

    @Override
    public IMqttToken subscribe(String[] topicFilters, int[] qos, Object userContext, IMqttActionListener callback, IMqttMessageListener[] messageListeners) throws MqttException {
        return getInstance().subscribe(topicFilters, qos, userContext, callback, messageListeners);
    }

    @Override
    public IMqttToken unsubscribe(String topicFilter) throws MqttException {
        return getInstance().unsubscribe(topicFilter);
    }

    @Override
    public IMqttToken unsubscribe(String[] topicFilters) throws MqttException {
        return getInstance().unsubscribe(topicFilters);
    }

    @Override
    public IMqttToken unsubscribe(String topicFilter, Object userContext, IMqttActionListener callback) throws MqttException {
        return getInstance().unsubscribe(topicFilter, userContext, callback);
    }

    @Override
    public IMqttToken unsubscribe(String[] topicFilters, Object userContext, IMqttActionListener callback) throws MqttException {
        return getInstance().unsubscribe(topicFilters, userContext, callback);
    }

    @Override
    public boolean removeMessage(IMqttDeliveryToken token) throws MqttException {
        return getInstance().removeMessage(token);
    }

    @Override
    public void setCallback(MqttCallback callback) {
        getInstance().setCallback(callback);
    }

    @Override
    public IMqttDeliveryToken[] getPendingDeliveryTokens() {
        return getInstance().getPendingDeliveryTokens();
    }

    @Override
    public void setManualAcks(boolean manualAcks) {
        getInstance().setManualAcks(manualAcks);
    }

    @Override
    public void reconnect() throws MqttException {
        getInstance().reconnect();
    }

    @Override
    public void messageArrivedComplete(int messageId, int qos) throws MqttException {
        getInstance().messageArrivedComplete(messageId, qos);
    }

    @Override
    public void setBufferOpts(DisconnectedBufferOptions bufferOpts) {
        getInstance().setBufferOpts(bufferOpts);
    }

    @Override
    public int getBufferedMessageCount() {
        return getInstance().getBufferedMessageCount();
    }

    @Override
    public MqttMessage getBufferedMessage(int bufferIndex) {
        return getInstance().getBufferedMessage(bufferIndex);
    }

    @Override
    public void deleteBufferedMessage(int bufferIndex) {
        getInstance().deleteBufferedMessage(bufferIndex);
    }

    @Override
    public int getInFlightMessageCount() {
        return getInstance().getInFlightMessageCount();
    }

    @Override
    public void close() throws MqttException {
        getInstance().close();
    }
}
