package com.paklog.inventory.application.port;

import com.paklog.inventory.domain.event.DomainEvent;

public interface EventPublisherPort {
    void publish(DomainEvent event);
}