package com.poc.pocpdf.application.port.out;

import java.nio.file.Path;
import java.util.List;

public interface DocxEditorPort {
    Path applyExtraClauses(Path templateDocx, Path outputDocx, List<String> extraClauses);
}
