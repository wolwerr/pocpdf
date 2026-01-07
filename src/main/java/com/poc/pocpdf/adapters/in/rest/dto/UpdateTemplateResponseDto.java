package com.poc.pocpdf.adapters.in.rest.dto;

public record UpdateTemplateResponseDto(
        String contractName,
        String version,
        String docxPath,
        String pdfPath
) {}
