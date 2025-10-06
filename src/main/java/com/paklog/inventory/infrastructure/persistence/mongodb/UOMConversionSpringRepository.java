package com.paklog.inventory.infrastructure.persistence.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data MongoDB repository for UOM conversions.
 */
@Repository
public interface UOMConversionSpringRepository extends MongoRepository<UOMConversionDocument, String> {

    List<UOMConversionDocument> findBySku(String sku);

    Optional<UOMConversionDocument> findBySkuAndFromUOM_CodeAndToUOM_Code(
        String sku, String fromUOMCode, String toUOMCode);

    void deleteBySkuAndFromUOM_CodeAndToUOM_Code(String sku, String fromUOMCode, String toUOMCode);

    boolean existsBySkuAndFromUOM_CodeAndToUOM_Code(String sku, String fromUOMCode, String toUOMCode);
}
