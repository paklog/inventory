package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.domain.model.Kit;
import com.paklog.inventory.domain.model.KitType;
import com.paklog.inventory.domain.repository.KitRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MongoDB implementation of KitRepository.
 */
@Component
public class KitRepositoryImpl implements KitRepository {

    private final KitSpringRepository springRepository;

    public KitRepositoryImpl(KitSpringRepository springRepository) {
        this.springRepository = springRepository;
    }

    @Override
    public Kit save(Kit kit) {
        KitDocument document = KitDocument.fromDomain(kit);
        KitDocument saved = springRepository.save(document);
        return saved.toDomain();
    }

    @Override
    public Optional<Kit> findByKitSku(String kitSku) {
        return springRepository.findByKitSku(kitSku)
            .map(KitDocument::toDomain);
    }

    @Override
    public List<Kit> findActiveKits() {
        return springRepository.findByActive(true).stream()
            .map(KitDocument::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Kit> findByType(KitType kitType) {
        return springRepository.findByKitType(kitType.name()).stream()
            .map(KitDocument::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Kit> findKitsContainingComponent(String componentSku) {
        return springRepository.findByComponentsSku(componentSku).stream()
            .map(KitDocument::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public boolean existsByKitSku(String kitSku) {
        return springRepository.existsByKitSku(kitSku);
    }

    @Override
    public void delete(String kitSku) {
        springRepository.deleteByKitSku(kitSku);
    }
}
