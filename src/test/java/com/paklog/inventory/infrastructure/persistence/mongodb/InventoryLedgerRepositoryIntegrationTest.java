package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.domain.model.ChangeType;
import com.paklog.inventory.domain.model.InventoryLedgerEntry;
import com.paklog.inventory.domain.repository.InventoryLedgerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for InventoryLedgerRepository.
 */
@DataMongoTest
@Testcontainers
@Import({InventoryLedgerRepositoryImpl.class})
class InventoryLedgerRepositoryIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
            .withExposedPorts(27017);

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private InventoryLedgerRepository ledgerRepository;

    @BeforeEach
    void setUp() {
        ledgerRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        ledgerRepository.deleteAll();
    }

    @Test
    @DisplayName("Should save ledger entry")
    void shouldSaveLedgerEntry() {
        // Given
        InventoryLedgerEntry entry = InventoryLedgerEntry.create(
                "SKU-001",
                50,
                ChangeType.RECEIPT,
                "PO-12345",
                "Purchase order received",
                "operator123"
        );

        // When
        InventoryLedgerEntry saved = ledgerRepository.save(entry);

        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getSku()).isEqualTo("SKU-001");
        assertThat(saved.getQuantityChange()).isEqualTo(50);
        assertThat(saved.getChangeType()).isEqualTo(ChangeType.RECEIPT);
    }

    @Test
    @DisplayName("Should find ledger entries by SKU")
    void shouldFindLedgerEntriesBySku() {
        // Given
        ledgerRepository.save(InventoryLedgerEntry.create("SKU-001", 50, ChangeType.RECEIPT, "PO-1", "Receipt", "op1"));
        ledgerRepository.save(InventoryLedgerEntry.create("SKU-001", -20, ChangeType.ALLOCATION, "SO-1", "Sale", "op1"));
        ledgerRepository.save(InventoryLedgerEntry.create("SKU-002", 100, ChangeType.RECEIPT, "PO-2", "Receipt", "op2"));

        // When
        List<InventoryLedgerEntry> entries = ledgerRepository.findBySku("SKU-001");

        // Then
        assertThat(entries).hasSize(2);
        assertThat(entries).allMatch(e -> e.getSku().equals("SKU-001"));
    }

    @Test
    @DisplayName("Should find ledger entries by SKU and time range")
    void shouldFindLedgerEntriesBySkuAndTimeRange() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);
        LocalDateTime twoDaysAgo = now.minusDays(2);

        ledgerRepository.save(InventoryLedgerEntry.load(
                null, "SKU-001", twoDaysAgo, 100, ChangeType.RECEIPT, "PO-1", "Old receipt", "op1"
        ));
        ledgerRepository.save(InventoryLedgerEntry.create("SKU-001", 50, ChangeType.RECEIPT, "PO-2", "Recent receipt", "op1"));
        ledgerRepository.save(InventoryLedgerEntry.create("SKU-001", -20, ChangeType.ALLOCATION, "SO-1", "Recent sale", "op1"));

        // When
        List<InventoryLedgerEntry> entries = ledgerRepository.findBySkuAndTimestampBetween(
                "SKU-001",
                yesterday,
                now.plusHours(1)
        );

        // Then
        assertThat(entries).hasSize(2);
        assertThat(entries).allMatch(e -> e.getTimestamp().isAfter(yesterday));
    }

    @Test
    @DisplayName("Should find ledger entries by change type")
    void shouldFindLedgerEntriesByChangeType() {
        // Given
        ledgerRepository.save(InventoryLedgerEntry.create("SKU-001", 50, ChangeType.RECEIPT, "PO-1", "Receipt", "op1"));
        ledgerRepository.save(InventoryLedgerEntry.create("SKU-002", 100, ChangeType.RECEIPT, "PO-2", "Receipt", "op1"));
        ledgerRepository.save(InventoryLedgerEntry.create("SKU-003", -20, ChangeType.ALLOCATION, "SO-1", "Sale", "op1"));
        ledgerRepository.save(InventoryLedgerEntry.create("SKU-004", 10, ChangeType.ADJUSTMENT, "ADJ-1", "Adjustment", "op1"));

        // When
        List<InventoryLedgerEntry> receipts = ledgerRepository.findByChangeType(ChangeType.RECEIPT);

        // Then
        assertThat(receipts).hasSize(2);
        assertThat(receipts).allMatch(e -> e.getChangeType() == ChangeType.RECEIPT);
    }

    @Test
    @DisplayName("Should return entries ordered by timestamp descending")
    void shouldReturnEntriesOrderedByTimestampDescending() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        InventoryLedgerEntry entry1 = InventoryLedgerEntry.load(
                null, "SKU-001", now.minusHours(3), 100, ChangeType.RECEIPT, "PO-1", "First", "op1"
        );
        InventoryLedgerEntry entry2 = InventoryLedgerEntry.load(
                null, "SKU-001", now.minusHours(2), 50, ChangeType.RECEIPT, "PO-2", "Second", "op1"
        );
        InventoryLedgerEntry entry3 = InventoryLedgerEntry.load(
                null, "SKU-001", now.minusHours(1), -20, ChangeType.ALLOCATION, "SO-1", "Third", "op1"
        );

        ledgerRepository.save(entry1);
        ledgerRepository.save(entry2);
        ledgerRepository.save(entry3);

        // When
        List<InventoryLedgerEntry> entries = ledgerRepository.findBySku("SKU-001");

        // Then
        assertThat(entries).hasSize(3);
        // Should be in descending order (most recent first)
        assertThat(entries.get(0).getReason()).isEqualTo("Third");
        assertThat(entries.get(1).getReason()).isEqualTo("Second");
        assertThat(entries.get(2).getReason()).isEqualTo("First");
    }

    @Test
    @DisplayName("Should handle negative quantity changes for allocations")
    void shouldHandleNegativeQuantityChanges() {
        // Given
        InventoryLedgerEntry allocation = InventoryLedgerEntry.create(
                "SKU-001",
                -30,
                ChangeType.ALLOCATION,
                "SO-001",
                "Sales order allocation",
                "operator123"
        );

        // When
        InventoryLedgerEntry saved = ledgerRepository.save(allocation);

        // Then
        assertThat(saved.getQuantityChange()).isEqualTo(-30);
        assertThat(saved.getChangeType()).isEqualTo(ChangeType.ALLOCATION);
    }

    @Test
    @DisplayName("Should save multiple entries in batch")
    void shouldSaveMultipleEntriesInBatch() {
        // Given
        List<InventoryLedgerEntry> entries = List.of(
                InventoryLedgerEntry.create("SKU-001", 100, ChangeType.RECEIPT, "PO-1", "Receipt 1", "op1"),
                InventoryLedgerEntry.create("SKU-002", 200, ChangeType.RECEIPT, "PO-2", "Receipt 2", "op1"),
                InventoryLedgerEntry.create("SKU-003", 300, ChangeType.RECEIPT, "PO-3", "Receipt 3", "op1")
        );

        // When
        List<InventoryLedgerEntry> saved = ledgerRepository.saveAll(entries);

        // Then
        assertThat(saved).hasSize(3);
        assertThat(ledgerRepository.findAll()).hasSize(3);
    }

    @Test
    @DisplayName("Should persist all entry fields correctly")
    void shouldPersistAllEntryFieldsCorrectly() {
        // Given
        InventoryLedgerEntry entry = InventoryLedgerEntry.create(
                "SKU-TEST-001",
                75,
                ChangeType.RECEIPT,
                "PO-99999",
                "Test purchase order",
                "test-operator"
        );

        // When
        ledgerRepository.save(entry);
        List<InventoryLedgerEntry> found = ledgerRepository.findBySku("SKU-TEST-001");

        // Then
        assertThat(found).hasSize(1);
        InventoryLedgerEntry savedEntry = found.get(0);
        assertThat(savedEntry.getSku()).isEqualTo("SKU-TEST-001");
        assertThat(savedEntry.getQuantityChange()).isEqualTo(75);
        assertThat(savedEntry.getChangeType()).isEqualTo(ChangeType.RECEIPT);
        assertThat(savedEntry.getSourceReference()).isEqualTo("PO-99999");
        assertThat(savedEntry.getReason()).isEqualTo("Test purchase order");
        assertThat(savedEntry.getOperatorId()).isEqualTo("test-operator");
        assertThat(savedEntry.getTimestamp()).isNotNull();
    }
}
