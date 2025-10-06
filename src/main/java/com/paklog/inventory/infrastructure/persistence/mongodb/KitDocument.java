package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.domain.model.Kit;
import com.paklog.inventory.domain.model.KitType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MongoDB document for Kit aggregate.
 */
@Document(collection = "kits")
@CompoundIndexes({
    @CompoundIndex(name = "sku_idx", def = "{'kitSku': 1}", unique = true),
    @CompoundIndex(name = "type_active_idx", def = "{'kitType': 1, 'active': 1}")
})
public class KitDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String kitSku;
    private String kitDescription;
    private List<KitComponentDocument> components;
    private String kitType;  // KitType as string
    private boolean allowPartialKit;
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;
    private boolean active;

    public KitDocument() {
    }

    public static KitDocument fromDomain(Kit kit) {
        KitDocument doc = new KitDocument();
        doc.kitSku = kit.getKitSku();
        doc.kitDescription = kit.getKitDescription();
        doc.components = kit.getComponents().stream()
            .map(KitComponentDocument::fromDomain)
            .collect(Collectors.toList());
        doc.kitType = kit.getKitType().name();
        doc.allowPartialKit = kit.isAllowPartialKit();
        doc.createdAt = kit.getCreatedAt();
        doc.lastModifiedAt = kit.getLastModifiedAt();
        doc.active = kit.isActive();
        return doc;
    }

    public Kit toDomain() {
        Kit kit = Kit.create(
            kitSku,
            kitDescription,
            components.stream().map(KitComponentDocument::toDomain).collect(Collectors.toList()),
            KitType.valueOf(kitType),
            allowPartialKit
        );

        if (!active) {
            kit.deactivate();
        }

        return kit;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKitSku() {
        return kitSku;
    }

    public void setKitSku(String kitSku) {
        this.kitSku = kitSku;
    }

    public String getKitDescription() {
        return kitDescription;
    }

    public void setKitDescription(String kitDescription) {
        this.kitDescription = kitDescription;
    }

    public List<KitComponentDocument> getComponents() {
        return components;
    }

    public void setComponents(List<KitComponentDocument> components) {
        this.components = components;
    }

    public String getKitType() {
        return kitType;
    }

    public void setKitType(String kitType) {
        this.kitType = kitType;
    }

    public boolean isAllowPartialKit() {
        return allowPartialKit;
    }

    public void setAllowPartialKit(boolean allowPartialKit) {
        this.allowPartialKit = allowPartialKit;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    public void setLastModifiedAt(LocalDateTime lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
