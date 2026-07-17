package com.backend.storage_service.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

@Component
@RequiredArgsConstructor
public class BucketInitializer {

    private final S3Client client;

    private final StorageProperties properties;

    @PostConstruct
    public void init() {

        boolean exists = client.listBuckets()
                .buckets()
                .stream()
                .anyMatch(b -> b.name().equals(properties.getBucket()));

        if (!exists) {

            client.createBucket(
                    CreateBucketRequest.builder()
                            .bucket(properties.getBucket())
                            .build()
            );

        }

    }
}
