package com.github.tocrhz.mqtt.exception;

import com.github.tocrhz.mqtt.subscriber.ParameterModel;

public class NullParameterException extends RuntimeException {

    public NullParameterException() {
        super("param is null");
    }

    public NullParameterException(ParameterModel parameter) {
        super("param name '" + parameter.getName() + "' type '" + parameter.getType().getName() + "' is required.");
    }
}
