package com.senim.backend.service;

import com.senim.backend.domain.ChecklistItem;
import com.senim.backend.domain.ChecklistStatus;
import com.senim.backend.domain.Deal;
import com.senim.backend.domain.Document;
import com.senim.backend.domain.User;
import com.senim.backend.dto.DocumentResponse;
import com.senim.backend.exception.BusinessRuleException;
import com.senim.backend.exception.ResourceNotFoundException;
import com.senim.backend.repository.ChecklistItemRepository;
import com.senim.backend.repository.DealRepository;
import com.senim.backend.repository.DocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class DocumentService {

    private static final long MAX_FILE_SIZE_BYTES = 20L * 1024 * 1024; // 20 MB

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("pdf", "jpg", "jpeg", "png", "docx");

    private final String uploadDirBase;
    private final DocumentRepository documentRepository;
    private final DealRepository dealRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final RiskEngineService riskEngineService;

    public DocumentService(
            @Value("${app.storage.upload-dir:./uploads}") String uploadDirBase,
            DocumentRepository documentRepository,
            DealRepository dealRepository,
            ChecklistItemRepository checklistItemRepository,
            RiskEngineService riskEngineService) {
        this.uploadDirBase = uploadDirBase;
        this.documentRepository = documentRepository;
        this.dealRepository = dealRepository;
        this.checklistItemRepository = checklistItemRepository;
        this.riskEngineService = riskEngineService;
    }

    /**
     * Validates and stores the uploaded file, persists the Document entity,
     * auto-matches checklist items by filename, then re-evaluates deal risk.
     */
    @Transactional
    public DocumentResponse uploadDocument(UUID dealId, MultipartFile file, User uploadedBy) {
        validateFile(file);

        Deal deal = loadDeal(dealId);
        assertSameAgency(deal, uploadedBy);

        String originalFilename = sanitize(file.getOriginalFilename());
        String extension = getExtension(originalFilename);
        String storedFilename = UUID.randomUUID() + "_" + originalFilename;

        Path targetDir = Paths.get(uploadDirBase,
                deal.getAgencyId().toString(),
                dealId.toString());

        try {
            Files.createDirectories(targetDir);
            Files.copy(file.getInputStream(), targetDir.resolve(storedFilename),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessRuleException("File storage failed: " + e.getMessage());
        }

        Document document = documentRepository.save(Document.builder()
                .dealId(dealId)
                .originalFilename(originalFilename)
                .storedFilename(storedFilename)
                .fileSizeBytes(file.getSize())
                .mimeType(resolveMimeType(extension, file.getContentType()))
                .uploadedByUserId(uploadedBy.getId())
                .build());

        autoMatchChecklist(dealId, document.getId(), originalFilename);
        riskEngineService.evaluateAndUpdateDeal(dealId);

        log.info("Document {} uploaded to deal {} by user {}",
                document.getId(), dealId, uploadedBy.getId());

        return DocumentResponse.from(document);
    }

    /**
     * Returns all documents for a deal.
     */
    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocumentsForDeal(UUID dealId) {
        return documentRepository.findAllByDealId(dealId).stream()
                .map(DocumentResponse::from)
                .toList();
    }

    /**
     * Returns the file as a streamable Resource. The caller is responsible for
     * setting Content-Type and Content-Disposition headers.
     */
    @Transactional(readOnly = true)
    public Resource downloadDocument(UUID documentId, User requestingUser) {
        Document document = loadDocument(documentId);
        Deal deal = loadDeal(document.getDealId());
        assertSameAgency(deal, requestingUser);

        Path filePath = Paths.get(uploadDirBase,
                deal.getAgencyId().toString(),
                document.getDealId().toString(),
                document.getStoredFilename());

        if (!Files.exists(filePath)) {
            throw new ResourceNotFoundException(
                    "File not found on storage for document: " + documentId);
        }

        return new FileSystemResource(filePath);
    }

    /**
     * Deletes the file from the filesystem and removes the Document entity,
     * then re-evaluates deal risk.
     */
    @Transactional
    public void deleteDocument(UUID documentId, User requestingUser) {
        Document document = loadDocument(documentId);
        Deal deal = loadDeal(document.getDealId());
        assertSameAgency(deal, requestingUser);

        Path filePath = Paths.get(uploadDirBase,
                deal.getAgencyId().toString(),
                document.getDealId().toString(),
                document.getStoredFilename());

        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Could not delete file {} from storage: {}", filePath, e.getMessage());
        }

        documentRepository.delete(document);
        riskEngineService.evaluateAndUpdateDeal(document.getDealId());

        log.info("Document {} deleted from deal {} by user {}",
                documentId, document.getDealId(), requestingUser.getId());
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Uploaded file is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BusinessRuleException("File exceeds the 20 MB size limit");
        }
        String ext = getExtension(sanitize(file.getOriginalFilename()));
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessRuleException(
                    "File type not allowed: ." + ext + ". Allowed: pdf, jpg, jpeg, png, docx");
        }
    }

    /**
     * Checks if any MISSING checklist item's itemKey appears in the uploaded filename
     * (case-insensitive substring match on the base name without extension).
     * When matched, the item is automatically transitioned to UPLOADED.
     */
    private void autoMatchChecklist(UUID dealId, UUID documentId, String originalFilename) {
        String fileBase = originalFilename.toLowerCase().replaceAll("\\.[^.]+$", "");

        List<ChecklistItem> candidates = checklistItemRepository
                .findAllByDealIdOrderBySortOrder(dealId).stream()
                .filter(item -> item.getStatus() == ChecklistStatus.MISSING
                        && fileBase.contains(item.getItemKey()))
                .toList();

        if (!candidates.isEmpty()) {
            candidates.forEach(item -> {
                item.setDocumentId(documentId);
                item.setStatus(ChecklistStatus.UPLOADED);
            });
            checklistItemRepository.saveAll(candidates);
            log.info("Auto-matched {} checklist item(s) for deal {} from file '{}'",
                    candidates.size(), dealId, originalFilename);
        }
    }

    private String sanitize(String filename) {
        if (filename == null || filename.isBlank()) {
            return "file";
        }
        return Paths.get(filename).getFileName().toString()
                .replaceAll("[^a-zA-Z0-9._\\-]", "_");
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toLowerCase() : "";
    }

    private String resolveMimeType(String extension, String contentType) {
        return switch (extension) {
            case "pdf"  -> "application/pdf";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png"  -> "image/png";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default -> contentType != null ? contentType : "application/octet-stream";
        };
    }

    private Document loadDocument(UUID documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
    }

    private Deal loadDeal(UUID dealId) {
        return dealRepository.findById(dealId)
                .orElseThrow(() -> new ResourceNotFoundException("Deal not found: " + dealId));
    }

    private void assertSameAgency(Deal deal, User user) {
        if (!deal.getAgencyId().equals(user.getAgencyId())) {
            throw new AccessDeniedException("You do not have access to this document");
        }
    }
}
