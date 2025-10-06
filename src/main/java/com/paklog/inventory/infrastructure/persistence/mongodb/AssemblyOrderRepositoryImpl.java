package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.domain.model.AssemblyOrder;
import com.paklog.inventory.domain.model.AssemblyStatus;
import com.paklog.inventory.domain.model.AssemblyType;
import com.paklog.inventory.domain.repository.AssemblyOrderRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MongoDB implementation of AssemblyOrderRepository.
 */
@Component
public class AssemblyOrderRepositoryImpl implements AssemblyOrderRepository {

    private final AssemblyOrderSpringRepository springRepository;

    public AssemblyOrderRepositoryImpl(AssemblyOrderSpringRepository springRepository) {
        this.springRepository = springRepository;
    }

    @Override
    public AssemblyOrder save(AssemblyOrder order) {
        AssemblyOrderDocument document = AssemblyOrderDocument.fromDomain(order);
        AssemblyOrderDocument saved = springRepository.save(document);
        return saved.toDomain();
    }

    @Override
    public Optional<AssemblyOrder> findById(String assemblyOrderId) {
        return springRepository.findByAssemblyOrderId(assemblyOrderId)
            .map(AssemblyOrderDocument::toDomain);
    }

    @Override
    public List<AssemblyOrder> findByKitSku(String kitSku) {
        return springRepository.findByKitSku(kitSku).stream()
            .map(AssemblyOrderDocument::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<AssemblyOrder> findByStatus(AssemblyStatus status) {
        return springRepository.findByStatus(status.name()).stream()
            .map(AssemblyOrderDocument::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<AssemblyOrder> findByType(AssemblyType type) {
        return springRepository.findByAssemblyType(type.name()).stream()
            .map(AssemblyOrderDocument::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<AssemblyOrder> findByTypeAndStatus(AssemblyType type, AssemblyStatus status) {
        return springRepository.findByAssemblyTypeAndStatus(type.name(), status.name()).stream()
            .map(AssemblyOrderDocument::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<AssemblyOrder> findInProgressOrders() {
        return findByStatus(AssemblyStatus.IN_PROGRESS);
    }

    @Override
    public long countByStatus(AssemblyStatus status) {
        return springRepository.countByStatus(status.name());
    }
}
