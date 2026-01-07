package com.poc.pocpdf.adapters.out.blob;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.specialized.BlobLeaseClient;
import com.azure.storage.blob.specialized.BlobLeaseClientBuilder;
import com.poc.pocpdf.application.port.out.LockPort;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.concurrent.Callable;

public class AzureBlobLeaseLockAdapter implements LockPort {

    private final BlobServiceClient serviceClient;
    private final String outputContainer;

    public AzureBlobLeaseLockAdapter(BlobServiceClient serviceClient, String outputContainer) {
        this.serviceClient = serviceClient;
        this.outputContainer = outputContainer;
    }

    private BlobContainerClient container() {
        BlobContainerClient c = serviceClient.getBlobContainerClient(outputContainer);
        if (!c.exists()) c.create();
        return c;
    }

    private String lockBlobName(String lockKey) {
        String safe = lockKey == null ? "lock" : lockKey.trim();
        safe = safe.replaceAll("[^a-zA-Z0-9_-]", "_");
        return "locks/" + safe + ".lock";
    }

    @Override
    public <T> T withLock(String lockKey, Duration ttl, Callable<T> action) {
        BlobClient lockBlob = container().getBlobClient(lockBlobName(lockKey));

        if (!lockBlob.exists()) {
            lockBlob.upload(new ByteArrayInputStream(new byte[0]), 0, true);
        }

        BlobLeaseClient leaseClient = new BlobLeaseClientBuilder()
                .blobClient(lockBlob)
                .buildClient();

        int seconds = (int) Math.max(15, Math.min(60, ttl == null ? 60 : ttl.getSeconds()));
        String leaseId = null;

        for (int attempt = 1; attempt <= 30; attempt++) {
            try {
                leaseId = leaseClient.acquireLease(seconds);
                break;
            } catch (BlobStorageException e) {
                if (e.getStatusCode() == 409) {
                    try {
                        Thread.sleep(200L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrompido aguardando lock.", ie);
                    }
                } else {
                    throw e;
                }
            }
        }

        if (leaseId == null) {
            throw new RuntimeException("Não foi possível adquirir lock para: " + lockKey);
        }

        try {
            return action.call();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            try {
                leaseClient.releaseLease();
            } catch (Exception ignore) {
            }
        }
    }
}
