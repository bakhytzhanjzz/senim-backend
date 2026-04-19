package com.senim.backend.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MarkUploadedRequest(

        @NotNull(message = "Document ID is required")
        UUID documentId
) {}
