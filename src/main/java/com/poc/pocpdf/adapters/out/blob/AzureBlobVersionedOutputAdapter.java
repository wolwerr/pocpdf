package com.poc.pocpdf.adapters.out.blob;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.poc.pocpdf.application.port.out.VersionedOutputPort;
import com.poc.pocpdf.domain.model.ContractName;
import com.poc.pocpdf.domain.model.Version;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AzureBlobVersionedOutputAdapter implements VersionedOutputPort {

    private final BlobServiceClient serviceClient;
    private final String outputContainer;

    public AzureBlobVersionedOutputAdapter(BlobServiceClient serviceClient, String outputContainer) {
        this.serviceClient = serviceClient;
        this.outputContainer = outputContainer;
    }

    private BlobContainerClient container() {
        BlobContainerClient c = serviceClient.getBlobContainerClient(outputContainer);
        if (!c.exists()) c.create();
        return c;
    }

    private String prefix(ContractName contractName) {
        return "contratos/" + contractName.asKey() + "/";
    }

    @Override
    public Version nextVersion(ContractName contractName) {
        String pfx = prefix(contractName);

        Pattern pat = Pattern.compile("^" + Pattern.quote(pfx) + "v(\\d+)/.*$");
        int max = 0;

        for (BlobItem item : container().listBlobs(new ListBlobsOptions().setPrefix(pfx), null)) {
            Matcher m = pat.matcher(item.getName());
            if (m.matches()) {
                int v = Integer.parseInt(m.group(1));
                if (v > max) max = v;
            }
        }

        return new Version(max + 1);
    }

    @Override
    public void save(ContractName contractName, Version version, String fileName, Path file) {
        if (file == null || !Files.exists(file)) {
            throw new IllegalArgumentException("Arquivo não existe: " + (file == null ? "null" : file.toAbsolutePath()));
        }

        String blobName = prefix(contractName) + version.asString() + "/" + fileName;

        BlobClient blob = container().getBlobClient(blobName);
        blob.uploadFromFile(file.toAbsolutePath().toString(), true);
    }
}
