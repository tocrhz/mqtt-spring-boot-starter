package com.github.tocrhz.mqtt.subscriber;

import com.github.tocrhz.mqtt.annotation.MqttSubscribe;

public class SubscriberModel {
    private final String[] value;
    private final int[] qos;
    private final String[] clients;
    private final String[] groups;

    public String[] value() {
        return this.value;
    }

    public int[] qos() {
        return this.qos;
    }

    public String[] clients() {
        return this.clients;
    }

    public String[] groups() {
        return this.groups;
    }

    public SubscriberModel(String[] value, int[] qos, String[] clients, String[] groups) {
        this.value = value == null ? new String[0] : value;
        this.qos = qos == null ? new int[0] : qos;
        this.clients = clients == null ? new String[0] : clients;
        this.groups = groups == null ? new String[0] : groups;
    }

    public static SubscriberModel of(MqttSubscribe subscribe) {
        return new SubscriberModel(subscribe.value(), subscribe.qos(), subscribe.clients(), subscribe.groups());
    }
}
