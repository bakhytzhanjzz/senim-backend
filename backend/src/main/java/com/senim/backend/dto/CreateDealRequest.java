package com.senim.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateDealRequest(

        @NotNull(message = "Agency ID is required")
        UUID agencyId,

        @NotBlank(message = "Client name is required")
        @Size(max = 255, message = "Client name must not exceed 255 characters")
        String clientName,

        @Size(max = 50, message = "Client phone must not exceed 50 characters")
        String clientPhone,

        @Size(max = 500, message = "Property address must not exceed 500 characters")
        String propertyAddress,

        @DecimalMin(value = "0.00", inclusive = false, message = "Property value must be positive")
        BigDecimal propertyValueUsd,

        @NotNull(message = "Commission amount is required")
        @DecimalMin(value = "0.00", inclusive = false, message = "Commission must be positive")
        BigDecimal commissionUsd,

        @Size(max = 255, message = "Bank name must not exceed 255 characters")
        String bankName,

        @Size(max = 255, message = "Mortgage program name must not exceed 255 characters")
        String mortgageProgramName,

        LocalDate bookingExpiryDate,

        LocalDate bankResponseDeadline,

        String notes
) {}
