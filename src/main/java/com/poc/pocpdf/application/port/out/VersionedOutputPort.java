package com.poc.pocpdf.application.port.out;

import com.poc.pocpdf.domain.model.ContractName;
import com.poc.pocpdf.domain.model.Version;

import java.nio.file.Path;

public interface VersionedOutputPort {

    Version nextVersion(ContractName contractName);

    void save(ContractName contractName, Version version, String fileName, Path file);
}
