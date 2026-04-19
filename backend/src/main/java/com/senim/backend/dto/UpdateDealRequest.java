package com.senim.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Editable deal fields. All values are optional; null means "do not change".
 * Status transitions are handled separately via PATCH /deals/{id}/status.
 */
public record UpdateDealRequest(
        @Size(max = 255) String clientName,
        @Size(max = 50) String clientPhone,
        @Size(max = 500) String propertyAddress,
        @DecimalMin(value = "0.00", inclusive = false) BigDecimal propertyValueUsd,
        @DecimalMin(value = "0.00", inclusive = false) BigDecimal commissionUsd,
        @Size(max = 255) String bankName,
        @Size(max = 255) String mortgageProgramName,
        LocalDate bookingExpiryDate,
        LocalDate bankResponseDeadline,
        String notes
) {}
