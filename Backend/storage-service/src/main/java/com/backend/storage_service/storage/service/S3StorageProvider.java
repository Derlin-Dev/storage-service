package com.backend.storage_service.storage.service;

import com.backend.storage_service.config.StorageProperties;
import com.backend.storage_service.storage.model.FileEntity;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;

@Service
public class S3StorageProvider implements StorageProvider {

    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final StorageProperties storageProperties;

    public S3StorageProvider(S3Client s3Client, S3Presigner presigner, StorageProperties storageProperties) {
        this.s3Client = s3Client;
        this.presigner = presigner;
        this.storageProperties = storageProperties;
    }

    @Override
    public String generateUploadUrl(FileEntity file) {

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(storageProperties.getBucket())
                .key(file.getObjectKey())
                .contentType(file.getContentType() != null
                        ? file.getContentType()
                        : "application/octet-stream")
                .build();

        PutObjectPresignRequest presignRequest =
                PutObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(10))
                        .putObjectRequest(objectRequest)
                        .build();

        PresignedPutObjectRequest presigned = presigner.presignPutObject(presignRequest);

        return presigned.url().toString();
    }

    @Override
    public String generateDownloadUrl(FileEntity file) {
        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket(storageProperties.getBucket())
                .key(file.getObjectKey())
                .build();


        GetObjectPresignRequest presignRequest =
                GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(10))
                        .getObjectRequest(objectRequest)
                        .build();


        PresignedGetObjectRequest presigned =
                presigner.presignGetObject(presignRequest);


        return presigned.url().toString();
    }

    @Override
    public void delete(FileEntity file) {

    }

    @Override
    public boolean exists(FileEntity file) {
        return false;
    }
}
