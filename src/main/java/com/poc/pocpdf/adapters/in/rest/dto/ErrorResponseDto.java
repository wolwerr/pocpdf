package com.poc.pocpdf.adapters.in.rest.dto;

import java.time.Instant;

public record ErrorResponseDto(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {}
