package com.github.tocrhz.mqtt.pojo.enums;

public enum QosEnum implements CodeEnum {

    /**
     * 最多一次的传输
     */
    QOS_LEVEL_0(0, "最多一次"),
    /**
     * 最多一次的传输
     */
    QOS_LEVEL_1(1, "至少一次"),
    /**
     * 最多一次的传输
     */
    QOS_LEVEL_2(2, "只有一次");

    private int code;
    private String name;

    QosEnum(int code, String name) {
        this.code = code;
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}
