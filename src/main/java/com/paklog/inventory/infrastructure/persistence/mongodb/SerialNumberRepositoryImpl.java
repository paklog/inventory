package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.domain.model.SerialNumber;
import com.paklog.inventory.domain.model.SerialStatus;
import com.paklog.inventory.domain.repository.SerialNumberRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MongoDB implementation of SerialNumberRepository
 */
@Component
public class SerialNumberRepositoryImpl implements SerialNumberRepository {

    private final SerialNumberSpringRepository springRepository;

    public SerialNumberRepositoryImpl(SerialNumberSpringRepository springRepository) {
        this.springRepository = springRepository;
    }

    @Override
    public SerialNumber save(SerialNumber serialNumber) {
        SerialNumberDocument doc = SerialNumberDocument.fromDomain(serialNumber);
        SerialNumberDocument saved = springRepository.save(doc);
        return saved.toDomain();
    }

    @Override
    public Optional<SerialNumber> findBySerialNumber(String serialNumber) {
        return springRepository.findBySerialNumber(serialNumber)
                .map(SerialNumberDocument::toDomain);
    }

    @Override
    public List<SerialNumber> findBySku(String sku) {
        return springRepository.findBySku(sku).stream()
                .map(SerialNumberDocument::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<SerialNumber> findBySkuAndStatus(String sku, SerialStatus status) {
        return springRepository.findBySkuAndStatus(sku, status.name()).stream()
                .map(SerialNumberDocument::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<SerialNumber> findAvailableBySku(String sku) {
        return findBySkuAndStatus(sku, SerialStatus.IN_INVENTORY);
    }

    @Override
    public List<SerialNumber> findByCustomerId(String customerId) {
        return springRepository.findByCustomerId(customerId).stream()
                .map(SerialNumberDocument::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsBySerialNumber(String serialNumber) {
        return springRepository.existsBySerialNumber(serialNumber);
    }

    @Override
    public void delete(SerialNumber serialNumber) {
        springRepository.findBySerialNumber(serialNumber.getSerialNumber())
                .ifPresent(springRepository::delete);
    }

    @Override
    public long countBySkuAndStatus(String sku, SerialStatus status) {
        return springRepository.countBySkuAndStatus(sku, status.name());
    }
}
