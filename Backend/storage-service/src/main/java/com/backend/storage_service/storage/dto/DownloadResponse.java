package com.backend.storage_service.storage.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DownloadResponse {
    private UUID fileId;
    private String fileName;
    private String contentType;
    private Long size;
    private String downloadUrl;
}
