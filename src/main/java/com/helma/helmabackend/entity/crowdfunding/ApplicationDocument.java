package com.helma.helmabackend.entity.crowdfunding;

import com.helma.helmabackend.entity.crowdfunding.enums.DocumentReviewStatus;
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

    public static final long MAX_FILE_SIZE_BYTES = 10L * 1024L * 1024L;

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

    @NotBlank(message = "storagePath is required")
    @Size(max = 800, message = "storagePath too long")
    @Column(name = "storage_path", nullable = false, length = 800)
    private String storagePath;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 30)
    private DocumentReviewStatus reviewStatus = DocumentReviewStatus.PENDING;

    @Column(name = "reviewed_by_user_id")
    private Long reviewedByUserId;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "review_note", columnDefinition = "TEXT")
    private String reviewNote;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (mimeType == null) {
            mimeType = "application/pdf";
        }
        if (reviewStatus == null) {
            reviewStatus = DocumentReviewStatus.PENDING;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
        if (reviewStatus == null) {
            reviewStatus = DocumentReviewStatus.PENDING;
        }
    }

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

    public DocumentReviewStatus getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(DocumentReviewStatus reviewStatus) { this.reviewStatus = reviewStatus; }

    public Long getReviewedByUserId() { return reviewedByUserId; }
    public void setReviewedByUserId(Long reviewedByUserId) { this.reviewedByUserId = reviewedByUserId; }

    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }

    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
