package com.paklog.inventory.infrastructure.operations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Automated MongoDB backup service.
 * Performs periodic backups using mongodump and manages backup retention.
 */
@Service
public class MongoBackupService {

    private static final Logger log = LoggerFactory.getLogger(MongoBackupService.class);
    private static final DateTimeFormatter BACKUP_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private boolean backupEnabled;

    private String backupDirectory;

    private int retentionDays;

    private String mongoUri;

    /**
     * Scheduled backup - runs daily at 2 AM
     */
    @Scheduled(cron = "${mongodb.backup.schedule:0 0 2 * * *}")
    public void performScheduledBackup() {
        if (!backupEnabled) {
            log.debug("MongoDB backup is disabled");
            return;
        }

        performBackup();
    }

    /**
     * Perform MongoDB backup using mongodump
     */
    public void performBackup() {
        String timestamp = LocalDateTime.now().format(BACKUP_DATE_FORMAT);
        String backupPath = String.format("%s/backup_%s", backupDirectory, timestamp);

        try {
            // Create backup directory if it doesn't exist
            Files.createDirectories(Paths.get(backupDirectory));

            log.info("Starting MongoDB backup to: {}", backupPath);

            // Build mongodump command
            List<String> command = new ArrayList<>();
            command.add("mongodump");
            command.add("--uri=" + mongoUri);
            command.add("--out=" + backupPath);
            command.add("--gzip"); // Compress backup

            // Execute mongodump
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();

            // Read output
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("mongodump: {}", line);
                }
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                log.info("MongoDB backup completed successfully: {}", backupPath);

                // Clean up old backups
                cleanupOldBackups();
            } else {
                log.error("MongoDB backup failed with exit code: {}", exitCode);
            }

        } catch (IOException | InterruptedException e) {
            log.error("Failed to perform MongoDB backup", e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Clean up backups older than retention period
     */
    private void cleanupOldBackups() {
        try {
            Path backupDir = Paths.get(backupDirectory);

            if (!Files.exists(backupDir)) {
                return;
            }

            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);

            Files.list(backupDir)
                    .filter(Files::isDirectory)
                    .filter(path -> isOlderThanCutoff(path, cutoffDate))
                    .forEach(path -> {
                        try {
                            deleteDirectory(path);
                            log.info("Deleted old backup: {}", path);
                        } catch (IOException e) {
                            log.error("Failed to delete old backup: {}", path, e);
                        }
                    });

        } catch (IOException e) {
            log.error("Failed to cleanup old backups", e);
        }
    }

    private boolean isOlderThanCutoff(Path path, LocalDateTime cutoffDate) {
        try {
            String dirName = path.getFileName().toString();
            if (dirName.startsWith("backup_")) {
                String dateStr = dirName.substring(7); // Remove "backup_" prefix
                LocalDateTime backupDate = LocalDateTime.parse(dateStr, BACKUP_DATE_FORMAT);
                return backupDate.isBefore(cutoffDate);
            }
        } catch (Exception e) {
            log.warn("Failed to parse backup directory date: {}", path, e);
        }
        return false;
    }

    private void deleteDirectory(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            Files.walk(path)
                    .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            log.error("Failed to delete: {}", p, e);
                        }
                    });
        }
    }

    /**
     * Restore from a specific backup
     */
    public void restoreFromBackup(String backupName) {
        String backupPath = String.format("%s/%s", backupDirectory, backupName);

        try {
            log.info("Starting MongoDB restore from: {}", backupPath);

            List<String> command = new ArrayList<>();
            command.add("mongorestore");
            command.add("--uri=" + mongoUri);
            command.add("--gzip");
            command.add("--drop"); // Drop collections before restoring
            command.add(backupPath);

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("mongorestore: {}", line);
                }
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                log.info("MongoDB restore completed successfully from: {}", backupPath);
            } else {
                log.error("MongoDB restore failed with exit code: {}", exitCode);
            }

        } catch (IOException | InterruptedException e) {
            log.error("Failed to restore MongoDB backup", e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * List available backups
     */
    public List<String> listAvailableBackups() {
        try {
            Path backupDir = Paths.get(backupDirectory);

            if (!Files.exists(backupDir)) {
                return List.of();
            }

            return Files.list(backupDir)
                    .filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .sorted((a, b) -> b.compareTo(a)) // Most recent first
                    .toList();

        } catch (IOException e) {
            log.error("Failed to list available backups", e);
            return List.of();
        

}
}
}
