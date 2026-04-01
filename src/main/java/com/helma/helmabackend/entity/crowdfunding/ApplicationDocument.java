package com.helma.helmabackend.entity.crowdfunding;

import com.helma.helmabackend.entity.crowdfunding.enums.DocumentType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.Instant;

@Entity
@Table(
        name = "application_document",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_app_doc_type", columnNames = {"application_raise_id", "doc_type"})
        },
        indexes = {
                @Index(name = "idx_app_doc_app", columnList = "application_raise_id")
        }
)
public class ApplicationDocument {

    // ====== Size policy ======
    // Choose 10MB as a good default for PDFs (pitch deck, scans, statements).
    // If you expect huge bank statements/scans, use 20MB.
    public static final long MAX_FILE_SIZE_BYTES = 10L * 1024L * 1024L; // 10 MB

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "applicationRaiseId is required")
    @Column(name = "application_raise_id", nullable = false)
    private Long applicationRaiseId;

    @NotNull(message = "uploaderUserId is required")
    @Column(name = "uploader_user_id", nullable = false)
    private Long uploaderUserId;

    @NotNull(message = "docType is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type", nullable = false, length = 60)
    private DocumentType docType;

    @NotBlank(message = "fileName is required")
    @Size(max = 255, message = "fileName too long")
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    // Where the file is stored: local path, S3 key, etc.
    @NotBlank(message = "storagePath is required")
    @Size(max = 800, message = "storagePath too long")
    @Column(name = "storage_path", nullable = false, length = 800)
    private String storagePath;

    // Always PDF for now
    @NotBlank(message = "mimeType is required")
    @Size(max = 100, message = "mimeType too long")
    @Pattern(regexp = "^application/pdf$", message = "Only PDF is allowed")
    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType = "application/pdf";

    @NotNull(message = "sizeBytes is required")
    @Min(value = 1, message = "sizeBytes must be > 0")
    @Max(value = MAX_FILE_SIZE_BYTES, message = "File too large (max 10MB)")
    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        if (mimeType == null) {
            mimeType = "application/pdf";
        }
    }

    // ===== Getters / Setters =====

    public Long getId() { return id; }

    public Long getApplicationRaiseId() { return applicationRaiseId; }
    public void setApplicationRaiseId(Long applicationRaiseId) { this.applicationRaiseId = applicationRaiseId; }

    public Long getUploaderUserId() { return uploaderUserId; }
    public void setUploaderUserId(Long uploaderUserId) { this.uploaderUserId = uploaderUserId; }

    public DocumentType getDocType() { return docType; }
    public void setDocType(DocumentType docType) { this.docType = docType; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }

    public Instant getCreatedAt() { return createdAt; }
}