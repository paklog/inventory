package com.paklog.inventory.infrastructure.persistence.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data MongoDB repository for SerialNumberDocument
 */
@Repository
public interface SerialNumberSpringRepository extends MongoRepository<SerialNumberDocument, String> {

    Optional<SerialNumberDocument> findBySerialNumber(String serialNumber);

    List<SerialNumberDocument> findBySku(String sku);

    List<SerialNumberDocument> findBySkuAndStatus(String sku, String status);

    List<SerialNumberDocument> findByCustomerId(String customerId);

    boolean existsBySerialNumber(String serialNumber);

    long countBySkuAndStatus(String sku, String status);
}
