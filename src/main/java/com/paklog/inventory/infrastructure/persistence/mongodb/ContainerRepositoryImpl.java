package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.domain.model.Container;
import com.paklog.inventory.domain.model.ContainerStatus;
import com.paklog.inventory.domain.model.ContainerType;
import com.paklog.inventory.domain.repository.ContainerRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MongoDB implementation of ContainerRepository
 */
@Component
public class ContainerRepositoryImpl implements ContainerRepository {

    private final ContainerSpringRepository springRepository;

    public ContainerRepositoryImpl(ContainerSpringRepository springRepository) {
        this.springRepository = springRepository;
    }

    @Override
    public Container save(Container container) {
        ContainerDocument doc = ContainerDocument.fromDomain(container);
        ContainerDocument saved = springRepository.save(doc);
        return saved.toDomain();
    }

    @Override
    public Optional<Container> findByLpn(String lpn) {
        return springRepository.findByLpn(lpn)
                .map(ContainerDocument::toDomain);
    }

    @Override
    public List<Container> findByStatus(ContainerStatus status) {
        return springRepository.findByStatus(status.name()).stream()
                .map(ContainerDocument::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Container> findByType(ContainerType type) {
        return springRepository.findByType(type.name()).stream()
                .map(ContainerDocument::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Container> findByTypeAndStatus(ContainerType type, ContainerStatus status) {
        return springRepository.findByTypeAndStatus(type.name(), status.name()).stream()
                .map(ContainerDocument::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Container> findActiveContainers() {
        return findByStatus(ContainerStatus.ACTIVE);
    }

    @Override
    public List<Container> findByLocation(String warehouseId, String zoneId) {
        return springRepository.findByLocation(warehouseId, zoneId).stream()
                .map(ContainerDocument::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Container> findByParentLpn(String parentLpn) {
        return springRepository.findByParentLpn(parentLpn).stream()
                .map(ContainerDocument::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Container> findEmptyContainers() {
        return springRepository.findEmptyContainers().stream()
                .map(ContainerDocument::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByLpn(String lpn) {
        return springRepository.existsByLpn(lpn);
    }

    @Override
    public void delete(Container container) {
        springRepository.deleteById(container.getLpn());
    }

    @Override
    public long countByStatus(ContainerStatus status) {
        return springRepository.countByStatus(status.name());
    }

    @Override
    public long countByType(ContainerType type) {
        return springRepository.countByType(type.name());
    }
}
