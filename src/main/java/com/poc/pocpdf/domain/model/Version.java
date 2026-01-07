package com.poc.pocpdf.domain.model;

public record Version(int number) {
    public Version {
        if (number < 1) throw new IllegalArgumentException("Versão deve ser >= 1");
    }
    public String asString() { return "v" + number; }
}
