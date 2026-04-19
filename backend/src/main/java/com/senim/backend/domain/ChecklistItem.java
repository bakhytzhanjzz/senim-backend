package com.senim.backend.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "checklist_items", indexes = {
        @Index(name = "idx_checklist_deal_id", columnList = "deal_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "deal_id", nullable = false)
    private UUID dealId;

    @Column(name = "item_name", nullable = false, length = 255)
    private String itemName;

    /**
     * Machine-readable key, e.g. "passport", "income_proof", "property_valuation".
     * Used for programmatic checklist initialization and matching.
     */
    @Column(name = "item_key", nullable = false, length = 100)
    private String itemKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ChecklistStatus status = ChecklistStatus.MISSING;

    @Column(name = "document_id")
    private UUID documentId;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "verified_by_user_id")
    private UUID verifiedByUserId;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;
}
