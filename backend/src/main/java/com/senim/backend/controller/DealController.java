package com.senim.backend.controller;

import com.senim.backend.domain.User;
import com.senim.backend.dto.CreateDealRequest;
import com.senim.backend.dto.DealResponse;
import com.senim.backend.dto.DealSummaryResponse;
import com.senim.backend.dto.UpdateDealStatusRequest;
import com.senim.backend.service.DealService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/deals")
public class DealController {

    private final DealService dealService;

    public DealController(DealService dealService) {
        this.dealService = dealService;
    }

    /**
     * Creates a new deal. Both AGENT and OWNER roles may create deals.
     */
    @PostMapping
    public ResponseEntity<DealResponse> createDeal(
            @Valid @RequestBody CreateDealRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dealService.createDeal(request, currentUser));
    }

    /**
     * Returns deals visible to the authenticated user.
     * OWNER: all agency deals. AGENT: own deals only.
     */
    @GetMapping
    public ResponseEntity<List<DealResponse>> getDeals(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(dealService.getDeals(currentUser));
    }

    /**
     * Returns a specific deal. User must belong to the same agency as the deal.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DealResponse> getDealById(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(dealService.getDealById(id, currentUser));
    }

    /**
     * Updates a deal's status. Enforces the state machine and the checklist
     * hard-gate when submitting.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<DealResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDealStatusRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                dealService.updateDealStatus(id, request.status(), currentUser));
    }

    /**
     * Risk dashboard summary — restricted to OWNER role.
     * Returns deal counts by status/risk level and commission at risk.
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<DealSummaryResponse> getDashboard(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                dealService.getDealSummaryForDashboard(currentUser.getAgencyId()));
    }
}
