package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.domain.model.UOMConversion;
import com.paklog.inventory.domain.model.UnitOfMeasure;
import com.paklog.inventory.domain.repository.UOMConversionRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MongoDB implementation of UOMConversionRepository.
 */
@Component
public class UOMConversionRepositoryImpl implements UOMConversionRepository {

    private final UOMConversionSpringRepository springRepository;

    public UOMConversionRepositoryImpl(UOMConversionSpringRepository springRepository) {
        this.springRepository = springRepository;
    }

    @Override
    public void save(String sku, UOMConversion conversion) {
        UOMConversionDocument document = UOMConversionDocument.fromDomain(sku, conversion);
        springRepository.save(document);
    }

    @Override
    public Optional<UOMConversion> findBySku(String sku, UnitOfMeasure fromUOM, UnitOfMeasure toUOM) {
        return springRepository.findBySkuAndFromUOM_CodeAndToUOM_Code(
                sku, fromUOM.getCode(), toUOM.getCode())
            .map(UOMConversionDocument::toDomain);
    }

    @Override
    public List<UOMConversion> findAllBySku(String sku) {
        return springRepository.findBySku(sku).stream()
            .map(UOMConversionDocument::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public void delete(String sku, UnitOfMeasure fromUOM, UnitOfMeasure toUOM) {
        springRepository.deleteBySkuAndFromUOM_CodeAndToUOM_Code(
            sku, fromUOM.getCode(), toUOM.getCode());
    }

    @Override
    public boolean exists(String sku, UnitOfMeasure fromUOM, UnitOfMeasure toUOM) {
        return springRepository.existsBySkuAndFromUOM_CodeAndToUOM_Code(
            sku, fromUOM.getCode(), toUOM.getCode());
    }
}
