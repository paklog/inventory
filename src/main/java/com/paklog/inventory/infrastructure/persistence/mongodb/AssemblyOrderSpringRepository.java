package com.paklog.inventory.infrastructure.persistence.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data MongoDB repository for Assembly Orders.
 */
@Repository
public interface AssemblyOrderSpringRepository extends MongoRepository<AssemblyOrderDocument, String> {

    Optional<AssemblyOrderDocument> findByAssemblyOrderId(String assemblyOrderId);

    List<AssemblyOrderDocument> findByKitSku(String kitSku);

    List<AssemblyOrderDocument> findByStatus(String status);

    List<AssemblyOrderDocument> findByAssemblyType(String assemblyType);

    List<AssemblyOrderDocument> findByAssemblyTypeAndStatus(String assemblyType, String status);

    long countByStatus(String status);
}
