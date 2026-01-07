package com.poc.pocpdf.config;

import com.azure.storage.blob.BlobServiceClient;
import com.poc.pocpdf.adapters.out.blob.AzureBlobLeaseLockAdapter;
import com.poc.pocpdf.adapters.out.blob.AzureBlobTemplateStorageAdapter;
import com.poc.pocpdf.adapters.out.blob.AzureBlobVersionedOutputAdapter;
import com.poc.pocpdf.adapters.out.docx.PoiDocxEditorAdapter;
import com.poc.pocpdf.adapters.out.pdf.LibreOfficePdfConverterAdapter;
import com.poc.pocpdf.application.port.in.UpdateTemplateUseCase;
import com.poc.pocpdf.application.port.out.DocxEditorPort;
import com.poc.pocpdf.application.port.out.LockPort;
import com.poc.pocpdf.application.port.out.PdfConverterPort;
import com.poc.pocpdf.application.port.out.TemplateStoragePort;
import com.poc.pocpdf.application.port.out.VersionedOutputPort;
import com.poc.pocpdf.application.service.UpdateTemplateService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PortsConfig {

    // ---- Outbound ports (interfaces) -> adapters ----
    @Bean
    public TemplateStoragePort templateStoragePort(BlobServiceClient client, AppProperties props) {
        return new AzureBlobTemplateStorageAdapter(
                client,
                props.getStorage().getTemplatesContainer(),
                props.getStorage().getOutputContainer()
        );
    }

    @Bean
    public VersionedOutputPort versionedOutputPort(BlobServiceClient client, AppProperties props) {
        return new AzureBlobVersionedOutputAdapter(client, props.getStorage().getOutputContainer());
    }

    @Bean
    public LockPort lockPort(BlobServiceClient client, AppProperties props) {
        return new AzureBlobLeaseLockAdapter(client, props.getStorage().getOutputContainer());
    }

    @Bean
    public DocxEditorPort docxEditorPort() {
        return new PoiDocxEditorAdapter();
    }

    @Bean
    public PdfConverterPort pdfConverterPort(AppProperties props) {
        return new LibreOfficePdfConverterAdapter(props.getLibreOffice().getSofficePath());
    }

    // ---- Inbound port (use case) ----
    @Bean
    public UpdateTemplateUseCase updateTemplateUseCase(
            TemplateStoragePort templateStorage,
            VersionedOutputPort versionedOutput,
            LockPort lockPort,
            DocxEditorPort docxEditor,
            PdfConverterPort pdfConverter,
            AppProperties props
    ) {
        return new UpdateTemplateService(
                templateStorage,
                versionedOutput,
                lockPort,
                docxEditor,
                pdfConverter,
                props.getStorage().getWorkDir()
        );
    }
}
