package com.poc.pocpdf.application.port.in;

import com.poc.pocpdf.domain.model.Clause;
import com.poc.pocpdf.domain.model.ContractName;
import com.poc.pocpdf.domain.event.TemplateVersionCreated;

import java.util.List;

public interface UpdateTemplateUseCase {
    TemplateVersionCreated updateTemplate(ContractName contractName,
                                          byte[] templateDocx,
                                          List<Clause> extraClauses);
}
