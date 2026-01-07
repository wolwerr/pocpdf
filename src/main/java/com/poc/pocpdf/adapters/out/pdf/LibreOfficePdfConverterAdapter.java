package com.poc.pocpdf.adapters.out.pdf;

import com.poc.pocpdf.application.port.out.PdfConverterPort;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class LibreOfficePdfConverterAdapter implements PdfConverterPort {

    private final String sofficePath;

    public LibreOfficePdfConverterAdapter(String sofficePath) {
        this.sofficePath = (sofficePath == null || sofficePath.isBlank()) ? "soffice" : sofficePath;
    }

    @Override
    public Path docxToPdf(Path docx, Path outDir) {
        try {
            Files.createDirectories(outDir);

            String baseName = docx.getFileName().toString().replaceAll("\\.docx$", "");
            Path expectedPdf = outDir.resolve(baseName + ".pdf");

            Path profileDir = outDir.resolve(".lo-profile-" + UUID.randomUUID());
            Files.createDirectories(profileDir);

            String userInstallation = profileDir.toUri().toString();

            ProcessBuilder pb = new ProcessBuilder(
                    sofficePath,
                    "--headless",
                    "--nologo",
                    "--nolockcheck",
                    "--nodefault",
                    "--nofirststartwizard",
                    "-env:UserInstallation=" + userInstallation,
                    "--convert-to", "pdf",
                    "--outdir", outDir.toAbsolutePath().toString(),
                    docx.toAbsolutePath().toString()
            );

            pb.redirectErrorStream(true);
            Process p = pb.start();

            StringBuilder log = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    log.append(line).append("\n");
                }
            }

            int code = p.waitFor();
            if (code != 0) {
                throw new RuntimeException("LibreOffice falhou. ExitCode=" + code + "\n" + log);
            }

            if (!Files.exists(expectedPdf)) {
                throw new RuntimeException("PDF não foi gerado. Log:\n" + log);
            }

            return expectedPdf;
        } catch (Exception e) {
            throw new RuntimeException("Falha ao converter DOCX para PDF via LibreOffice.", e);
        }
    }
}
