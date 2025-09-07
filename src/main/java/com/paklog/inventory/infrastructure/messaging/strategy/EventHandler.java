package com.paklog.inventory.infrastructure.messaging.strategy;

import io.cloudevents.CloudEvent;

public interface EventHandler {
    boolean canHandle(String eventType);
    void handle(CloudEvent event) throws Exception;
}