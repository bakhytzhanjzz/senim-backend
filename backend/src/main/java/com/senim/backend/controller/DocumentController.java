package com.senim.backend.controller;

import com.senim.backend.domain.User;
import com.senim.backend.dto.DocumentResponse;
import com.senim.backend.service.DocumentService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * Uploads a document to a deal. Accepts multipart/form-data with a "file" part.
     * Max file size and allowed types are enforced by DocumentService.
     */
    @PostMapping("/api/v1/deals/{dealId}/documents")
    public ResponseEntity<DocumentResponse> uploadDocument(
            @PathVariable UUID dealId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.uploadDocument(dealId, file, currentUser));
    }

    /**
     * Lists all documents for a deal.
     */
    @GetMapping("/api/v1/deals/{dealId}/documents")
    public ResponseEntity<List<DocumentResponse>> listDocuments(
            @PathVariable UUID dealId) {
        return ResponseEntity.ok(documentService.getDocumentsForDeal(dealId));
    }

    /**
     * Streams the file as a download. Content-Disposition is set to attachment
     * using the original filename so the browser saves it with the correct name.
     */
    @GetMapping("/api/v1/documents/{documentId}/download")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal User currentUser) {
        Resource resource = documentService.downloadDocument(documentId, currentUser);

        String contentDisposition = "attachment; filename=\""
                + resource.getFilename() + "\"";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    /**
     * Deletes a document and removes it from the filesystem.
     * Re-evaluates the deal's risk level after deletion.
     */
    @DeleteMapping("/api/v1/documents/{documentId}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal User currentUser) {
        documentService.deleteDocument(documentId, currentUser);
        return ResponseEntity.noContent().build();
    }
}
