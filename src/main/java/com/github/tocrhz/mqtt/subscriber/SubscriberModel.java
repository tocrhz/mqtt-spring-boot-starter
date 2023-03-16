package com.github.tocrhz.mqtt.subscriber;

import com.github.tocrhz.mqtt.annotation.MqttSubscribe;

public class SubscriberModel {
    public SubscriberModel(String[] value, int[] qos, String[] clients, boolean[] shared, String[] groups) {
        this.value = value == null ? new String[0] : value;
        this.qos = qos == null ? new int[0] : qos;
        this.clients = clients == null ? new String[0] : clients;
        this.shared = shared == null ? new boolean[0] : shared;
        this.groups = groups == null ? new String[0] : groups;
    }

    public static SubscriberModel of(MqttSubscribe subscribe) {
        return new SubscriberModel(subscribe.value(), subscribe.qos(), subscribe.clients(), subscribe.shared(), subscribe.groups());
    }

    private final String[] value;
    private final int[] qos;
    private final String[] clients;
    private final boolean[] shared;
    private final String[] groups;

    public String[] value() {
        return value;
    }

    public int[] qos() {
        return qos;
    }

    public String[] clients() {
        return clients;
    }

    public boolean[] shared() {
        return shared;
    }

    public String[] groups() {
        return groups;
    }
}
