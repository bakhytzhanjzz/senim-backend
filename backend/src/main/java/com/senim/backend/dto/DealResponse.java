package com.senim.backend.dto;

import com.senim.backend.domain.Deal;
import com.senim.backend.domain.DealStatus;
import com.senim.backend.domain.RiskLevel;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public record DealResponse(
        UUID id,
        UUID agencyId,
        UUID agentId,
        String clientName,
        String clientPhone,
        String propertyAddress,
        BigDecimal propertyValueUsd,
        BigDecimal commissionUsd,
        String bankName,
        String mortgageProgramName,
        DealStatus status,
        RiskLevel riskLevel,
        LocalDate bookingExpiryDate,
        LocalDate bankResponseDeadline,
        Long daysUntilBookingExpiry,
        Long daysUntilBankDeadline,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    public static DealResponse from(Deal deal) {
        return new DealResponse(
                deal.getId(),
                deal.getAgencyId(),
                deal.getAgentId(),
                deal.getClientName(),
                deal.getClientPhone(),
                deal.getPropertyAddress(),
                deal.getPropertyValueUsd(),
                deal.getCommissionUsd(),
                deal.getBankName(),
                deal.getMortgageProgramName(),
                deal.getStatus(),
                deal.getRiskLevel(),
                deal.getBookingExpiryDate(),
                deal.getBankResponseDeadline(),
                daysUntil(deal.getBookingExpiryDate()),
                daysUntil(deal.getBankResponseDeadline()),
                deal.getNotes(),
                deal.getCreatedAt(),
                deal.getUpdatedAt()
        );
    }

    private static Long daysUntil(LocalDate date) {
        if (date == null) {
            return null;
        }
        long days = ChronoUnit.DAYS.between(LocalDate.now(), date);
        return days >= 0 ? days : null;
    }
}
