package com.poc.pocpdf.domain.model;

public record Clause(String text) {
    public Clause {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Cláusula não pode ser vazia.");
        }
    }
}
