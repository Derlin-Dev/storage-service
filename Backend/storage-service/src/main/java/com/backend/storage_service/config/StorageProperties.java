package com.backend.storage_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "storage")
@Getter
@Setter
public class StorageProperties {
    private String endpoint;

    private String region;

    private String bucket;

    private String accessKey;

    private String secretKey;

    public boolean isCustomEndpoint() {
        return endpoint != null && !endpoint.isBlank();
    }
}
