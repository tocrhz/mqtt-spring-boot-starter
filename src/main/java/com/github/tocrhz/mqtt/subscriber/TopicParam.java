package com.github.tocrhz.mqtt.subscriber;

/**
 * @author tjheiska
 */
class TopicParam {
    private String name;
    private int at;

    public TopicParam(String name, int at) {
        super();
        this.name = name;
        this.at = at;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAt() {
        return at;
    }

    public void setAt(int at) {
        this.at = at;
    }
}
