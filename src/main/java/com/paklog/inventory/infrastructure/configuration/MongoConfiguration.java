package com.paklog.inventory.infrastructure.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "com.paklog.inventory.infrastructure.persistence.mongodb")
public class MongoConfiguration {
    // Custom configurations can be added here if needed,
    // but Spring Boot's auto-configuration is often sufficient.
}