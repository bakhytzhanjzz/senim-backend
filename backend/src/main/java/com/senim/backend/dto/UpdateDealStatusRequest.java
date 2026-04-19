package com.senim.backend.dto;

import com.senim.backend.domain.DealStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateDealStatusRequest(

        @NotNull(message = "Status is required")
        DealStatus status
) {}
