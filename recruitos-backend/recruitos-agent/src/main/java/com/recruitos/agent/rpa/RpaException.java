package com.recruitos.agent.rpa;

public class RpaException extends RuntimeException {

    public RpaException(String message) {
        super(message);
    }

    public RpaException(String message, Throwable cause) {
        super(message, cause);
    }
}
