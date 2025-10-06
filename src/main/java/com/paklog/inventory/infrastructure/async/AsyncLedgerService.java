package com.paklog.inventory.infrastructure.async;

import com.paklog.inventory.domain.model.InventoryLedgerEntry;
import com.paklog.inventory.domain.repository.InventoryLedgerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Async service for ledger operations that can be processed in background.
 * Improves response time by not blocking on audit trail writes.
 */
@Service
public class AsyncLedgerService {

    private static final Logger log = LoggerFactory.getLogger(AsyncLedgerService.class);

    private final InventoryLedgerRepository ledgerRepository;

    public AsyncLedgerService(InventoryLedgerRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    /**
     * Asynchronously save ledger entry for audit trail.
     * Returns CompletableFuture for caller to wait if needed.
     *
     * @param entry Ledger entry to save
     * @return CompletableFuture that completes when entry is saved
     */
    @Async("taskExecutor")
    public CompletableFuture<InventoryLedgerEntry> saveLedgerEntryAsync(InventoryLedgerEntry entry) {
        try {
            log.debug("Async saving ledger entry for sku: {}, type: {}",
                    entry.getSku(), entry.getChangeType());
            InventoryLedgerEntry saved = ledgerRepository.save(entry);
            return CompletableFuture.completedFuture(saved);
        } catch (Exception e) {
            log.error("Failed to save ledger entry asynchronously for sku: {}",
                    entry.getSku(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Fire-and-forget ledger entry save.
     * Use only when caller doesn't need to know about save completion.
     */
    @Async("taskExecutor")
    public void saveLedgerEntryFireAndForget(InventoryLedgerEntry entry) {
        try {
            log.debug("Fire-and-forget saving ledger entry for sku: {}", entry.getSku());
            ledgerRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to save ledger entry (fire-and-forget) for sku: {}",
                    entry.getSku(), e);
            // Don't propagate - this is fire-and-forget
        }
    }
}
