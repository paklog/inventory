package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.mongodb.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monitors MongoDB connection pool events for observability.
 * Logs connection lifecycle events and pool statistics.
 */
public class MongoConnectionPoolMonitor implements ConnectionPoolListener {

    private static final Logger log = LoggerFactory.getLogger(MongoConnectionPoolMonitor.class);

    @Override
    public void connectionPoolCreated(ConnectionPoolCreatedEvent event) {
        log.info("MongoDB connection pool created: serverId={}, settings={}",
                event.getServerId(), event.getSettings());
    }

    @Override
    public void connectionPoolCleared(ConnectionPoolClearedEvent event) {
        log.warn("MongoDB connection pool cleared: serverId={}, serviceId={}",
                event.getServerId(), event.getServiceId());
    }

    @Override
    public void connectionPoolClosed(ConnectionPoolClosedEvent event) {
        log.info("MongoDB connection pool closed: serverId={}",
                event.getServerId());
    }

    @Override
    public void connectionCheckOutStarted(ConnectionCheckOutStartedEvent event) {
        log.debug("MongoDB connection checkout started: serverId={}",
                event.getServerId());
    }

    @Override
    public void connectionCheckedOut(ConnectionCheckedOutEvent event) {
        log.debug("MongoDB connection checked out: connectionId={}",
                event.getConnectionId());
    }

    @Override
    public void connectionCheckOutFailed(ConnectionCheckOutFailedEvent event) {
        log.error("MongoDB connection checkout failed: serverId={}, reason={}",
                event.getServerId(), event.getReason());
    }

    @Override
    public void connectionCheckedIn(ConnectionCheckedInEvent event) {
        log.debug("MongoDB connection checked in: connectionId={}",
                event.getConnectionId());
    }

    @Override
    public void connectionCreated(ConnectionCreatedEvent event) {
        log.debug("MongoDB connection created: connectionId={}",
                event.getConnectionId());
    }

    @Override
    public void connectionReady(ConnectionReadyEvent event) {
        log.debug("MongoDB connection ready: connectionId={}",
                event.getConnectionId());
    }

    @Override
    public void connectionClosed(ConnectionClosedEvent event) {
        log.debug("MongoDB connection closed: connectionId={}, reason={}",
                event.getConnectionId(), event.getReason());
    }
}
