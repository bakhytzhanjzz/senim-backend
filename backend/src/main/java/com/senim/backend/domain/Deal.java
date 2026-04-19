package com.senim.backend.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "deals", indexes = {
        @Index(name = "idx_deals_agency_id", columnList = "agency_id"),
        @Index(name = "idx_deals_agent_id", columnList = "agent_id"),
        @Index(name = "idx_deals_status", columnList = "status"),
        @Index(name = "idx_deals_risk_level", columnList = "risk_level")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Deal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "agency_id", nullable = false)
    private UUID agencyId;

    @Column(name = "agent_id", nullable = false)
    private UUID agentId;

    @Column(name = "client_name", nullable = false, length = 255)
    private String clientName;

    @Column(name = "client_phone", length = 50)
    private String clientPhone;

    @Column(name = "property_address", length = 500)
    private String propertyAddress;

    @Column(name = "property_value_usd", precision = 18, scale = 2)
    private BigDecimal propertyValueUsd;

    @Column(name = "commission_usd", nullable = false, precision = 18, scale = 2)
    private BigDecimal commissionUsd;

    @Column(name = "bank_name", length = 255)
    private String bankName;

    @Column(name = "mortgage_program_name", length = 255)
    private String mortgageProgramName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DealStatus status = DealStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    @Builder.Default
    private RiskLevel riskLevel = RiskLevel.LOW;

    @Column(name = "booking_expiry_date")
    private LocalDate bookingExpiryDate;

    @Column(name = "bank_response_deadline")
    private LocalDate bankResponseDeadline;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * Tracks when the deal last changed status, used by StaleDealRule
     * to determine if a deal has been IN_PROGRESS for too long.
     */
    @Column(name = "status_changed_at")
    private Instant statusChangedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
