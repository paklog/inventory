package com.paklog.inventory.infrastructure.persistence.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data MongoDB repository for Kits.
 */
@Repository
public interface KitSpringRepository extends MongoRepository<KitDocument, String> {

    Optional<KitDocument> findByKitSku(String kitSku);

    List<KitDocument> findByActive(boolean active);

    List<KitDocument> findByKitType(String kitType);

    @Query("{ 'components.componentSku': ?0 }")
    List<KitDocument> findByComponentsSku(String componentSku);

    boolean existsByKitSku(String kitSku);

    void deleteByKitSku(String kitSku);
}
