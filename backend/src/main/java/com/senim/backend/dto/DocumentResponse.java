package com.senim.backend.dto;

import com.senim.backend.domain.Document;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        UUID dealId,
        String originalFilename,
        String mimeType,
        Long fileSizeBytes,
        Instant uploadedAt,
        UUID uploadedByUserId
) {
    public static DocumentResponse from(Document doc) {
        return new DocumentResponse(
                doc.getId(),
                doc.getDealId(),
                doc.getOriginalFilename(),
                doc.getMimeType(),
                doc.getFileSizeBytes(),
                doc.getUploadedAt(),
                doc.getUploadedByUserId()
        );
    }
}
