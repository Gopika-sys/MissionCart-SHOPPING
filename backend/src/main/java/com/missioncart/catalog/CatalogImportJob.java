package com.missioncart.catalog;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "catalog_import_jobs")
public class CatalogImportJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_platform", nullable = false, length = 60)
    private String sourcePlatform;

    @Column(nullable = false, length = 30)
    private String format;

    @Column(name = "file_name")
    private String fileName;

    @Column(nullable = false, length = 40)
    private String status = "COMPLETED";

    @Column(name = "total_records", nullable = false)
    private int totalRecords;

    @Column(name = "inserted_count", nullable = false)
    private int insertedCount;

    @Column(name = "updated_count", nullable = false)
    private int updatedCount;

    @Column(name = "skipped_duplicate_count", nullable = false)
    private int skippedDuplicateCount;

    @Column(name = "error_count", nullable = false)
    private int errorCount;

    @Column(name = "errors_json", columnDefinition = "TEXT")
    private String errorsJson;

    @Column(name = "initiated_by", nullable = false, length = 190)
    private String initiatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt = Instant.now();

    public Long getId() { return id; }
    public String getSourcePlatform() { return sourcePlatform; }
    public void setSourcePlatform(String sourcePlatform) { this.sourcePlatform = sourcePlatform; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getTotalRecords() { return totalRecords; }
    public void setTotalRecords(int totalRecords) { this.totalRecords = totalRecords; }
    public int getInsertedCount() { return insertedCount; }
    public void setInsertedCount(int insertedCount) { this.insertedCount = insertedCount; }
    public int getUpdatedCount() { return updatedCount; }
    public void setUpdatedCount(int updatedCount) { this.updatedCount = updatedCount; }
    public int getSkippedDuplicateCount() { return skippedDuplicateCount; }
    public void setSkippedDuplicateCount(int skippedDuplicateCount) { this.skippedDuplicateCount = skippedDuplicateCount; }
    public int getErrorCount() { return errorCount; }
    public void setErrorCount(int errorCount) { this.errorCount = errorCount; }
    public String getErrorsJson() { return errorsJson; }
    public void setErrorsJson(String errorsJson) { this.errorsJson = errorsJson; }
    public String getInitiatedBy() { return initiatedBy; }
    public void setInitiatedBy(String initiatedBy) { this.initiatedBy = initiatedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
