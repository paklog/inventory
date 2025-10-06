package com.paklog.inventory.infrastructure.persistence.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data MongoDB repository for ContainerDocument
 */
@Repository
public interface ContainerSpringRepository extends MongoRepository<ContainerDocument, String> {

    Optional<ContainerDocument> findByLpn(String lpn);

    List<ContainerDocument> findByStatus(String status);

    List<ContainerDocument> findByType(String type);

    List<ContainerDocument> findByTypeAndStatus(String type, String status);

    @Query("{ 'currentLocation.warehouseId': ?0, 'currentLocation.zoneId': ?1 }")
    List<ContainerDocument> findByLocation(String warehouseId, String zoneId);

    List<ContainerDocument> findByParentLpn(String parentLpn);

    @Query("{ 'status': 'EMPTY' }")
    List<ContainerDocument> findEmptyContainers();

    boolean existsByLpn(String lpn);

    long countByStatus(String status);

    long countByType(String type);
}
