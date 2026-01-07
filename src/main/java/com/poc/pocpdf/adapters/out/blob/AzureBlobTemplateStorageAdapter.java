package com.poc.pocpdf.adapters.out.blob;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.poc.pocpdf.application.port.out.TemplateStoragePort;
import com.poc.pocpdf.domain.model.ContractName;

import java.io.ByteArrayInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AzureBlobTemplateStorageAdapter implements TemplateStoragePort {

    private final BlobServiceClient serviceClient;
    private final String templatesContainer;
    private final String outputContainer;

    // versions ficam no outputContainer
    private static final Pattern VERSION_PATTERN = Pattern.compile("/v(\\d+)/", Pattern.CASE_INSENSITIVE);

    public AzureBlobTemplateStorageAdapter(BlobServiceClient serviceClient,
                                           String templatesContainer,
                                           String outputContainer) {
        this.serviceClient = serviceClient;
        this.templatesContainer = templatesContainer;
        this.outputContainer = outputContainer;
    }

    private BlobContainerClient templates() {
        BlobContainerClient c = serviceClient.getBlobContainerClient(templatesContainer);
        if (!c.exists()) c.create();
        return c;
    }

    private BlobContainerClient output() {
        BlobContainerClient c = serviceClient.getBlobContainerClient(outputContainer);
        if (!c.exists()) c.create();
        return c;
    }

    private String baseTemplateBlobName(ContractName contractName) {
        return contractName.asKey() + ".docx";
    }

    @Override
    public boolean exists(ContractName contractName) {
        // “existe template atual” = existe pelo menos uma versão vN
        return findLatestVersionDocxBlobName(contractName) != null;
    }

    @Override
    public void save(ContractName contractName, byte[] docxBytes) {
        // salva o template BASE
        if (docxBytes == null || docxBytes.length == 0) {
            throw new IllegalArgumentException("Template DOCX vazio.");
        }

        BlobClient blob = templates().getBlobClient(baseTemplateBlobName(contractName));
        try {
            blob.upload(new ByteArrayInputStream(docxBytes), docxBytes.length, true);
        } catch (BlobStorageException e) {
            throw new RuntimeException("Falha ao salvar template base no Blob: " + baseTemplateBlobName(contractName), e);
        }
    }

    @Override
    public byte[] load(ContractName contractName) {
        // carrega SEMPRE a ÚLTIMA versão vN do output
        String latest = findLatestVersionDocxBlobName(contractName);

        if (latest == null) {
            throw new IllegalArgumentException("Nenhuma versão encontrada para contrato: " + contractName.value());
        }

        try {
            return output().getBlobClient(latest).downloadContent().toBytes();
        } catch (BlobStorageException e) {
            throw new RuntimeException("Falha ao carregar template do Blob para contrato: " + contractName.value(), e);
        }
    }

    private String findLatestVersionDocxBlobName(ContractName contractName) {
        String key = contractName.asKey();
        String prefix = "contratos/" + key + "/"; // dentro do outputContainer

        int bestVersion = -1;
        String bestBlobName = null;

        Iterable<BlobItem> items = output().listBlobs(new ListBlobsOptions().setPrefix(prefix), null);

        for (BlobItem item : items) {
            if (item == null) continue;

            String name = item.getName();
            if (name == null) continue;

            if (!name.toLowerCase().endsWith(".docx")) continue;

            Matcher m = VERSION_PATTERN.matcher(name);
            if (!m.find()) continue;

            int v;
            try {
                v = Integer.parseInt(m.group(1));
            } catch (NumberFormatException e) {
                continue;
            }

            if (v > bestVersion) {
                bestVersion = v;
                bestBlobName = name;
            }
        }

        return bestBlobName;
    }
}
