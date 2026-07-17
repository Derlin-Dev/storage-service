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
public class UploadResponse {

    private UUID fileId;

    private String uploadUrl;

    private String objectKey;
}
