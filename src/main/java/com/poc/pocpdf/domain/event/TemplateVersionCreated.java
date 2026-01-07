package com.poc.pocpdf.domain.event;

import com.poc.pocpdf.domain.model.ContractName;
import com.poc.pocpdf.domain.model.Version;

public record TemplateVersionCreated(
        ContractName contractName,
        Version version,
        String docxPath,
        String pdfPath
) {}
