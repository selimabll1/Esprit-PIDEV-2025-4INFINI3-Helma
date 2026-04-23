package com.helma.helmabackend.service.crowdfunding;

import com.helma.helmabackend.dto.crowdfunding.ApplicationDocumentResponse;
import com.helma.helmabackend.entity.crowdfunding.ApplicationDocument;
import com.helma.helmabackend.entity.crowdfunding.ApplicationRaise;
import com.helma.helmabackend.entity.crowdfunding.enums.ApplicationRaiseStatus;
import com.helma.helmabackend.entity.crowdfunding.enums.CrowdfundingType;
import com.helma.helmabackend.entity.crowdfunding.enums.DocumentType;
import com.helma.helmabackend.entity.user.Role;
import com.helma.helmabackend.entity.user.User;
import com.helma.helmabackend.exception.UnauthorizedException;
import com.helma.helmabackend.repository.crowdfunding.ApplicationDocumentRepository;
import com.helma.helmabackend.repository.crowdfunding.ApplicationRaiseRepository;
import com.helma.helmabackend.service.user.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicationDocumentService {

    private final ApplicationDocumentRepository docRepo;
    private final ApplicationRaiseRepository appRepo;
    private final CurrentUserService currentUserService;

    @Value("${helma.storage.docs-dir:uploads/docs}")
    private String docsDir;

    private static final Set<DocumentType> COMMON_DOCS = EnumSet.of(
            DocumentType.ID_CARD,
            DocumentType.PROJECT_PITCH_DECK
    );

    private static final Set<DocumentType> EQUITY_REQUIRED_DOCS = EnumSet.of(
            DocumentType.CNRE_EXTRACT,
            DocumentType.SHAREHOLDERS_CAP_TABLE,
            DocumentType.FINANCIAL_STATEMENTS,
            DocumentType.BANK_RIB
    );

    @Transactional
    public ApplicationDocumentResponse uploadOwnerDocument(
            Long applicationRaiseId,
            DocumentType docType,
            MultipartFile file
    ) {
        User me = currentUser();

        if (me.getRole() != Role.YOUTH_BENEFICIARY) {
            throw new UnauthorizedException("Only YOUTH_BENEFICIARY can upload documents.");
        }
        if (docType == null) {
            throw new IllegalArgumentException("Document type is required.");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Document file is required.");
        }

        ApplicationRaise app = appRepo.findById(applicationRaiseId)
                .orElseThrow(() -> new IllegalArgumentException("ApplicationRaise not found: " + applicationRaiseId));

        if (!app.getOwnerUserId().equals(me.getId())) {
            throw new UnauthorizedException("You can only upload documents for your own application.");
        }
        if (app.getStatus() != ApplicationRaiseStatus.DRAFT) {
            throw new IllegalStateException("Documents can only be uploaded while application is DRAFT.");
        }

        validateDocTypeAllowed(app.getType(), docType);
        validatePdfOnly(file.getContentType());
        validateSize(file.getSize());

        String originalFileName = safeName(file.getOriginalFilename());
        String storagePath = storePdfToDisk(applicationRaiseId, docType, file);

        ApplicationDocument doc = docRepo.findByApplicationRaiseIdAndDocType(applicationRaiseId, docType)
                .orElseGet(() -> {
                    ApplicationDocument d = new ApplicationDocument();
                    d.setApplicationRaiseId(applicationRaiseId);
                    d.setUploaderUserId(me.getId());
                    d.setDocType(docType);
                    return d;
                });

        String oldStoragePath = doc.getStoragePath();

        doc.setFileName(originalFileName.isBlank() ? docType.name() + ".pdf" : originalFileName);
        doc.setStoragePath(storagePath);
        doc.setMimeType("application/pdf");
        doc.setSizeBytes(file.getSize());

        ApplicationDocument saved = docRepo.save(doc);

        if (oldStoragePath != null && !oldStoragePath.isBlank() && !oldStoragePath.equals(storagePath)) {
            deletePhysicalFileQuietly(oldStoragePath);
        }

        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ApplicationDocumentResponse> listDocumentResponses(Long applicationRaiseId) {
        User me = currentUser();

        ApplicationRaise app = appRepo.findById(applicationRaiseId)
                .orElseThrow(() -> new IllegalArgumentException("ApplicationRaise not found: " + applicationRaiseId));

        boolean isOwner = app.getOwnerUserId().equals(me.getId());
        boolean isAdminOrCompliance = isAdminOrCompliance(me);

        if (!isOwner && !isAdminOrCompliance) {
            throw new UnauthorizedException("You can only view documents for your own application.");
        }

        return toDtoList(docRepo.findByApplicationRaiseId(applicationRaiseId));
    }

    @Transactional
    public void deleteOwnerDocument(Long applicationRaiseId, DocumentType docType) {
        User me = currentUser();

        if (me.getRole() != Role.YOUTH_BENEFICIARY) {
            throw new UnauthorizedException("Only YOUTH_BENEFICIARY can delete documents.");
        }

        ApplicationRaise app = appRepo.findById(applicationRaiseId)
                .orElseThrow(() -> new IllegalArgumentException("ApplicationRaise not found: " + applicationRaiseId));

        if (!app.getOwnerUserId().equals(me.getId())) {
            throw new UnauthorizedException("You can only delete documents for your own application.");
        }
        if (app.getStatus() != ApplicationRaiseStatus.DRAFT) {
            throw new IllegalStateException("Documents can only be deleted while application is DRAFT.");
        }

        ApplicationDocument doc = docRepo.findByApplicationRaiseIdAndDocType(applicationRaiseId, docType)
                .orElseThrow(() -> new IllegalArgumentException("Document not found for type: " + docType));

        deletePhysicalFileQuietly(doc.getStoragePath());
        docRepo.delete(doc);
        cleanupApplicationDirIfEmpty(applicationRaiseId);
    }

    @Transactional
    public void deleteAllDocumentsForApplication(Long applicationRaiseId) {
        List<ApplicationDocument> docs = docRepo.findByApplicationRaiseId(applicationRaiseId);
        for (ApplicationDocument doc : docs) {
            deletePhysicalFileQuietly(doc.getStoragePath());
        }
        docRepo.deleteAll(docs);
        cleanupApplicationDirIfEmpty(applicationRaiseId);
    }

    @Transactional(readOnly = true)
    public void assertEquityDocsCompleteOrThrow(Long applicationRaiseId) {
        ApplicationRaise app = appRepo.findById(applicationRaiseId)
                .orElseThrow(() -> new IllegalArgumentException("ApplicationRaise not found: " + applicationRaiseId));

        if (app.getType() != CrowdfundingType.EQUITY) {
            return;
        }

        Set<DocumentType> present = docRepo.findByApplicationRaiseId(applicationRaiseId)
                .stream()
                .map(ApplicationDocument::getDocType)
                .collect(java.util.stream.Collectors.toSet());

        for (DocumentType required : EQUITY_REQUIRED_DOCS) {
            if (!present.contains(required)) {
                throw new IllegalStateException("Missing required EQUITY document: " + required);
            }
        }
    }

    public ApplicationDocumentResponse toDto(ApplicationDocument doc) {
        ApplicationDocumentResponse response = new ApplicationDocumentResponse();
        response.id = doc.getId();
        response.applicationRaiseId = doc.getApplicationRaiseId();
        response.docType = doc.getDocType();
        response.fileName = doc.getFileName();
        response.mimeType = doc.getMimeType();
        response.sizeBytes = doc.getSizeBytes();
        response.createdAt = doc.getCreatedAt();
        return response;
    }

    public List<ApplicationDocumentResponse> toDtoList(List<ApplicationDocument> docs) {
        return docs.stream()
                .map(this::toDto)
                .toList();
    }

    private void validateDocTypeAllowed(CrowdfundingType appType, DocumentType docType) {
        if (appType == CrowdfundingType.DONATION) {
            if (!COMMON_DOCS.contains(docType)) {
                throw new IllegalStateException(
                        "Document type " + docType + " is not allowed for DONATION applications."
                );
            }
            return;
        }

        if (appType == CrowdfundingType.EQUITY) {
            if (COMMON_DOCS.contains(docType) || EQUITY_REQUIRED_DOCS.contains(docType)) {
                return;
            }
            throw new IllegalStateException(
                    "Document type " + docType + " is not allowed for EQUITY applications."
            );
        }

        throw new IllegalStateException("Unsupported crowdfunding type: " + appType);
    }

    private String storePdfToDisk(Long applicationRaiseId, DocumentType docType, MultipartFile file) {
        try {
            Path base = Paths.get(docsDir).toAbsolutePath().normalize();
            Path appDir = base.resolve("application_" + applicationRaiseId);
            Files.createDirectories(appDir);

            Path target = appDir.resolve(docType.name() + "__" + UUID.randomUUID() + ".pdf");
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return target.toString();
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to store document: " + safeName(file.getOriginalFilename()),
                    e
            );
        }
    }

    private void deletePhysicalFileQuietly(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(storagePath));
        } catch (IOException ignored) {
        }
    }

    private void cleanupApplicationDirIfEmpty(Long applicationRaiseId) {
        try {
            Path base = Paths.get(docsDir).toAbsolutePath().normalize();
            Path appDir = base.resolve("application_" + applicationRaiseId);
            if (Files.exists(appDir)) {
                Files.delete(appDir);
            }
        } catch (DirectoryNotEmptyException ignored) {
        } catch (IOException ignored) {
        }
    }

    private User currentUser() {
        return currentUserService.getCurrentUser();
    }

    private boolean isAdminOrCompliance(User user) {
        return user.getRole() == Role.ADMIN || user.getRole() == Role.COMPLIANCE;
    }

    private void validatePdfOnly(String mimeType) {
        String value = mimeType == null ? "" : mimeType.trim();
        if (!"application/pdf".equalsIgnoreCase(value)) {
            throw new IllegalArgumentException("Only PDF files are allowed (application/pdf).");
        }
    }

    private void validateSize(long sizeBytes) {
        if (sizeBytes <= 0) {
            throw new IllegalArgumentException("File size must be > 0.");
        }
        if (sizeBytes > ApplicationDocument.MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException(
                    "File too large. Max is " + ApplicationDocument.MAX_FILE_SIZE_BYTES + " bytes."
            );
        }
    }

    private String safeName(String value) {
        return value == null ? "" : value.trim();
    }

    @Transactional(readOnly = true)
    public ApplicationDocument getReadableDocument(Long applicationRaiseId, DocumentType docType) {
        User me = currentUser();

        ApplicationRaise app = appRepo.findById(applicationRaiseId)
                .orElseThrow(() -> new IllegalArgumentException("ApplicationRaise not found: " + applicationRaiseId));

        boolean isOwner = app.getOwnerUserId().equals(me.getId());
        boolean isAdminOrCompliance = isAdminOrCompliance(me);

        if (!isOwner && !isAdminOrCompliance) {
            throw new UnauthorizedException("You can only view documents for your own application.");
        }

        return docRepo.findByApplicationRaiseIdAndDocType(applicationRaiseId, docType)
                .orElseThrow(() -> new IllegalArgumentException("Document not found for type: " + docType));
    }

    @Transactional(readOnly = true)
    public byte[] readDocumentBytes(ApplicationDocument doc) {
        try {
            return Files.readAllBytes(Paths.get(doc.getStoragePath()));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read document: " + doc.getDocType(), e);
        }
    }

    @Transactional(readOnly = true)
    public Path resolveReadableDocumentPath(ApplicationDocument doc) {
        if (doc == null || doc.getStoragePath() == null || doc.getStoragePath().isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document storage path is missing.");
        }

        Path path = Paths.get(doc.getStoragePath()).toAbsolutePath().normalize();

        if (!Files.exists(path)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Document file not found on disk: " + path
            );
        }

        if (!Files.isRegularFile(path)) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Stored document path is not a file: " + path
            );
        }

        if (!Files.isReadable(path)) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Document exists but is not readable: " + path
            );
        }

        return path;
    }
}