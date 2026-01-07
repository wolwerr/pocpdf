package com.poc.pocpdf.application.port.out;

import java.nio.file.Path;

public interface PdfConverterPort {
    Path docxToPdf(Path docx, Path outDir);
}
