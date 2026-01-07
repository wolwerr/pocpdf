package com.poc.pocpdf.application.port.out;

import com.poc.pocpdf.domain.model.ContractName;

public interface TemplateStoragePort {

    boolean exists(ContractName contractName);

    void save(ContractName contractName, byte[] docxBytes);

    byte[] load(ContractName contractName);
}
