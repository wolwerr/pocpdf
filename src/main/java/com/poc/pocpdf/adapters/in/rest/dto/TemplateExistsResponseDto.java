package com.poc.pocpdf.adapters.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TemplateExistsResponse")
public record TemplateExistsResponseDto(
        @Schema(example = "contrato-locacao") String contractName,
        @Schema(example = "true") boolean exists
) {}
