package com.poc.pocpdf.application.service;

import com.poc.pocpdf.application.port.in.UpdateTemplateUseCase;
import com.poc.pocpdf.application.port.out.*;
import com.poc.pocpdf.domain.model.Clause;
import com.poc.pocpdf.domain.model.ContractName;
import com.poc.pocpdf.domain.event.TemplateVersionCreated;
import com.poc.pocpdf.domain.model.Version;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

public class UpdateTemplateService implements UpdateTemplateUseCase {

    private final TemplateStoragePort templateStorage;
    private final VersionedOutputPort versionedOutput;
    private final LockPort lockPort;
    private final DocxEditorPort docxEditor;
    private final PdfConverterPort pdfConverter;
    private final String workDir;

    public UpdateTemplateService(TemplateStoragePort templateStorage,
                                 VersionedOutputPort versionedOutput,
                                 LockPort lockPort,
                                 DocxEditorPort docxEditor,
                                 PdfConverterPort pdfConverter,
                                 String workDir) {
        this.templateStorage = templateStorage;
        this.versionedOutput = versionedOutput;
        this.lockPort = lockPort;
        this.docxEditor = docxEditor;
        this.pdfConverter = pdfConverter;
        this.workDir = (workDir == null || workDir.isBlank()) ? "out/work" : workDir;
    }

    @Override
    public TemplateVersionCreated updateTemplate(ContractName contractName,
                                                 byte[] templateDocx,
                                                 List<Clause> extraClauses) {

        String key = contractName.asKey();
        String lockKey = "contract-template:" + key;

        return lockPort.withLock(lockKey, Duration.ofSeconds(60), () -> {

            // 1) atualiza o template "corrente" no container templates
            templateStorage.save(contractName, templateDocx);

            // 2) define próxima versão (v1, v2...)
            Version version = versionedOutput.nextVersion(contractName);

            // 3) prepara diretório de trabalho local
            Path baseDir = Paths.get(workDir, key, version.asString());
            Files.createDirectories(baseDir);

            Path inputDocx = baseDir.resolve(key + "-input.docx");
            Path outputDocx = baseDir.resolve(key + ".docx");

            Files.write(inputDocx, templateDocx);

            // 4) aplica cláusulas extras no docx
            List<String> clauses = (extraClauses == null)
                    ? List.of()
                    : extraClauses.stream().map(Clause::text).toList();

            docxEditor.applyExtraClauses(inputDocx, outputDocx, clauses);

            // 5) converte para PDF
            Path pdf = pdfConverter.docxToPdf(outputDocx, baseDir);

            // 6) salva versão no Blob
            versionedOutput.save(contractName, version, key + ".docx", outputDocx);
            versionedOutput.save(contractName, version, key + ".pdf", pdf);

            // 7) monta paths (para retornar na API)
            String docxPath = "output/contratos/" + key + "/" + version.asString() + "/" + key + ".docx";
            String pdfPath  = "output/contratos/" + key + "/" + version.asString() + "/" + key + ".pdf";

            return new TemplateVersionCreated(contractName, version, docxPath, pdfPath);
        });
    }
}
