package com.poc.pocpdf.config;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureClientConfig {

    @Bean
    public BlobServiceClient blobServiceClient(AppProperties props) {
        String cs = props.getAzure().getConnectionString();

        if (cs == null || cs.isBlank()) {
            throw new IllegalStateException(
                    "Connection string vazia. Defina app.azure.connectionString no application.yml " +
                            "ou AZURE_STORAGE_CONNECTION_STRING nas variáveis de ambiente."
            );
        }

        return new BlobServiceClientBuilder()
                .connectionString(cs.trim())
                .buildClient();
    }
}
