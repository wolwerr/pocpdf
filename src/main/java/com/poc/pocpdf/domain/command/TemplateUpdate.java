package com.poc.pocpdf.domain.command;

import com.poc.pocpdf.domain.model.Clause;
import com.poc.pocpdf.domain.model.ContractName;

import java.util.List;

public record TemplateUpdate(
        ContractName contractName,
        List<Clause> extraClauses
) {
    public TemplateUpdate {
        if (contractName == null) throw new IllegalArgumentException("contractName obrigatório.");
        if (extraClauses == null) extraClauses = List.of();
    }
}
