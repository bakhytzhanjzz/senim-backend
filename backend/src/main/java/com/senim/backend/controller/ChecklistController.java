package com.senim.backend.controller;

import com.senim.backend.domain.User;
import com.senim.backend.dto.ChecklistItemResponse;
import com.senim.backend.dto.MarkUploadedRequest;
import com.senim.backend.service.ChecklistService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/deals/{dealId}/checklist")
public class ChecklistController {

    private final ChecklistService checklistService;

    public ChecklistController(ChecklistService checklistService) {
        this.checklistService = checklistService;
    }

    /**
     * Returns the full checklist for a deal, ordered by sortOrder.
     */
    @GetMapping
    public ResponseEntity<List<ChecklistItemResponse>> getChecklist(
            @PathVariable UUID dealId) {
        return ResponseEntity.ok(checklistService.getChecklistForDeal(dealId));
    }

    /**
     * Associates an uploaded document with a checklist item, transitioning it to UPLOADED.
     * Both AGENT and OWNER may upload.
     */
    @PatchMapping("/{itemId}/upload")
    public ResponseEntity<ChecklistItemResponse> markUploaded(
            @PathVariable UUID dealId,
            @PathVariable UUID itemId,
            @Valid @RequestBody MarkUploadedRequest request) {
        return ResponseEntity.ok(checklistService.markItemUploaded(itemId, request.documentId()));
    }

    /**
     * Marks a checklist item as VERIFIED. Restricted to OWNER role.
     */
    @PatchMapping("/{itemId}/verify")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ChecklistItemResponse> markVerified(
            @PathVariable UUID dealId,
            @PathVariable UUID itemId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(checklistService.markItemVerified(itemId, currentUser.getId()));
    }
}
