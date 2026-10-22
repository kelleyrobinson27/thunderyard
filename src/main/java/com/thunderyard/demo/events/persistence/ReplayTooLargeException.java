package com.thunderyard.demo.events.persistence;

public class ReplayTooLargeException extends RuntimeException {

    public static final int MAX_EVENTS = 50;

    public ReplayTooLargeException(int requested) {
        super("Replay limited to " + MAX_EVENTS + " events (got " + requested + ")");
    }
}
