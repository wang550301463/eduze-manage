package com.eduze.platform.runtime;

/** Consumer business effect runs in the same transaction as its inbox receipt. */
public interface EventHandler {
    boolean supports(String type);

    void handle(EventEnvelope event);
}
