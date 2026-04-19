package com.senim.backend.dto;

import com.senim.backend.domain.ChecklistItem;
import com.senim.backend.domain.ChecklistStatus;

import java.time.Instant;
import java.util.UUID;

public record ChecklistItemResponse(
        UUID id,
        UUID dealId,
        String itemName,
        String itemKey,
        ChecklistStatus status,
        UUID documentId,
        Instant verifiedAt,
        UUID verifiedByUserId,
        int sortOrder
) {
    public static ChecklistItemResponse from(ChecklistItem item) {
        return new ChecklistItemResponse(
                item.getId(),
                item.getDealId(),
                item.getItemName(),
                item.getItemKey(),
                item.getStatus(),
                item.getDocumentId(),
                item.getVerifiedAt(),
                item.getVerifiedByUserId(),
                item.getSortOrder()
        );
    }
}
