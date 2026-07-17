package com.backend.storage_service.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@RequiredArgsConstructor
public class S3Config {

    private final StorageProperties properties;

    @Bean
    public AwsCredentialsProvider credentialsProvider() {

        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                        properties.getAccessKey(),
                        properties.getSecretKey()
                )
        );
    }

    @Bean
    public S3Client s3Client(AwsCredentialsProvider credentialsProvider) {

        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(credentialsProvider);

        if (properties.isCustomEndpoint()) {
            builder.endpointOverride(URI.create(properties.getEndpoint()))
                    .forcePathStyle(true);
        }

        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner(AwsCredentialsProvider credentialsProvider) {

        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(credentialsProvider);

        if (properties.isCustomEndpoint()) {

            builder.endpointOverride(
                    URI.create(properties.getEndpoint())
            );

            builder.serviceConfiguration(
                    S3Configuration.builder()
                            .pathStyleAccessEnabled(true)
                            .build()
            );
        }

        return builder.build();

    }
}
