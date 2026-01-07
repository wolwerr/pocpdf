package com.poc.pocpdf.adapters.out.docx;

import com.poc.pocpdf.application.port.out.DocxEditorPort;
import org.apache.poi.xwpf.usermodel.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PoiDocxEditorAdapter implements DocxEditorPort {

    private static final String PLACEHOLDER = "{{CLAUSULAS_EXTRAS}}";

    private static final Pattern CLAUSE_NUM_PATTERN =
            Pattern.compile("Cláusula adicional\\s+(\\d+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern PREFIX_PATTERN =
            Pattern.compile("^\\s*Cláusula adicional\\s+\\d+\\s*:\\s*", Pattern.CASE_INSENSITIVE);

    @Override
    public Path applyExtraClauses(Path templateDocx, Path outputDocx, List<String> extraClauses) {
        try (InputStream in = Files.newInputStream(templateDocx);
             XWPFDocument doc = new XWPFDocument(in)) {

            int startNumber = findMaxExistingClauseNumber(doc) + 1;

            boolean done = applyInParagraphs(doc.getParagraphs(), extraClauses, startNumber);

            if (!done) {
                for (XWPFTable table : doc.getTables()) {
                    for (XWPFTableRow row : table.getRows()) {
                        for (XWPFTableCell cell : row.getTableCells()) {
                            if (applyInParagraphs(cell.getParagraphs(), extraClauses, startNumber)) {
                                done = true;
                                break;
                            }
                        }
                        if (done) break;
                    }
                    if (done) break;
                }
            }

            Path parent = outputDocx.getParent();
            if (parent != null) Files.createDirectories(parent);

            try (OutputStream out = Files.newOutputStream(outputDocx)) {
                doc.write(out);
            }

            return outputDocx;
        } catch (Exception e) {
            throw new RuntimeException("Falha ao editar DOCX com POI.", e);
        }
    }

    private boolean applyInParagraphs(List<XWPFParagraph> paragraphs, List<String> clauses, int startNumber) {
        for (XWPFParagraph p : paragraphs) {
            if (!paragraphContains(p, PLACEHOLDER)) continue;

            replacePlaceholderWithClausesRuns(p, clauses, startNumber);
            return true;
        }
        return false;
    }

    private boolean paragraphContains(XWPFParagraph p, String token) {
        String text = p.getText();
        return text != null && text.contains(token);
    }

    private void replacePlaceholderWithClausesRuns(XWPFParagraph p, List<String> clauses, int startNumber) {
        if (clauses == null) clauses = List.of();

        // achar run que contém o marcador
        int markerRunIdx = findRunIndexContaining(p, PLACEHOLDER);

        // fallback: marcador pode ter sido quebrado em vários runs
        // nesse caso, simplifica: limpa os runs e recria um run-base mantendo estilo do 1o run
        if (markerRunIdx < 0) {
            if (!paragraphContains(p, PLACEHOLDER)) return;

            String font = null;
            Integer fontSize = null;
            if (!p.getRuns().isEmpty()) {
                XWPFRun r0 = p.getRuns().get(0);
                font = r0.getFontFamily();
                fontSize = r0.getFontSize();
            }

            int runs = p.getRuns().size();
            for (int i = runs - 1; i >= 0; i--) p.removeRun(i);

            XWPFRun base = p.createRun();
            if (font != null) base.setFontFamily(font);
            if (fontSize != null && fontSize > 0) base.setFontSize(fontSize);

            base.setText(""); // marcador vira vazio
            markerRunIdx = 0;
        } else {
            // remove o marcador só do run que o contém, preservando o resto do parágrafo
            XWPFRun markerRun = p.getRuns().get(markerRunIdx);
            String t = markerRun.getText(0);
            if (t != null && t.contains(PLACEHOLDER)) {
                markerRun.setText(t.replace(PLACEHOLDER, ""), 0);
            }
        }

        // fonte/tamanho base para manter igual ao template
        String font = null;
        Integer fontSize = null;
        if (!p.getRuns().isEmpty()) {
            XWPFRun r0 = p.getRuns().get(0);
            font = r0.getFontFamily();
            fontSize = r0.getFontSize();
        }

        int insertPos = markerRunIdx + 1;
        int n = startNumber;

        // insere cláusulas como runs no MESMO parágrafo, com quebra de linha entre elas
        for (int i = 0; i < clauses.size(); i++) {
            String raw = clauses.get(i);
            if (raw == null) continue;

            raw = raw.trim();
            if (raw.isBlank()) continue;

            // se vier "Cláusula adicional 100: ..." remove o prefixo, vamos renumerar
            String text = PREFIX_PATTERN.matcher(raw).replaceFirst("").trim();

            // Run 1: "Cláusula adicional N: " em negrito
            XWPFRun title = p.insertNewRun(insertPos++);
            if (font != null) title.setFontFamily(font);
            if (fontSize != null && fontSize > 0) title.setFontSize(fontSize);
            title.setBold(true);
            title.setText("Cláusula adicional " + n + ": ");

            // Run 2: texto normal
            XWPFRun body = p.insertNewRun(insertPos++);
            if (font != null) body.setFontFamily(font);
            if (fontSize != null && fontSize > 0) body.setFontSize(fontSize);
            body.setBold(false);
            body.setText(text);

            // quebra de linha depois de cada cláusula (exceto a última válida)
            boolean last = (i == clauses.size() - 1);
            if (!last) {
                body.addBreak();
                body.addBreak();
            }
            n++;
        }
    }

    private int findRunIndexContaining(XWPFParagraph p, String token) {
        List<XWPFRun> runs = p.getRuns();
        for (int i = 0; i < runs.size(); i++) {
            String t = runs.get(i).getText(0);
            if (t != null && t.contains(token)) return i;
        }
        return -1;
    }

    private int findMaxExistingClauseNumber(XWPFDocument doc) {
        int max = 0;

        max = Math.max(max, findMaxInParagraphs(doc.getParagraphs()));

        for (XWPFTable table : doc.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    max = Math.max(max, findMaxInParagraphs(cell.getParagraphs()));
                }
            }
        }

        return max;
    }

    private int findMaxInParagraphs(List<XWPFParagraph> paragraphs) {
        int max = 0;

        for (XWPFParagraph p : paragraphs) {
            String t = p.getText();
            if (t == null || t.isBlank()) continue;

            Matcher m = CLAUSE_NUM_PATTERN.matcher(t);
            while (m.find()) {
                int v = Integer.parseInt(m.group(1));
                if (v > max) max = v;
            }
        }

        return max;
    }
}
