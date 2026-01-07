package com.poc.pocpdf.domain.model;

public record ContractName(String value) {
    public ContractName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("contractName não pode ser vazio.");
        }
        value = value.trim();
    }

    public String asKey() {
        return value.trim()
                .toLowerCase()
                .replaceAll("\\s+", "-")
                .replaceAll("[^a-z0-9_-]", "");
    }
}
