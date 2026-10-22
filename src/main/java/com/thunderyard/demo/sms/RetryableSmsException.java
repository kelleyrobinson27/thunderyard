package com.thunderyard.demo.sms;

public class RetryableSmsException extends RuntimeException {

    public RetryableSmsException(String message) {
        super(message);
    }

    public RetryableSmsException(String message, Throwable cause) {
        super(message, cause);
    }
}
