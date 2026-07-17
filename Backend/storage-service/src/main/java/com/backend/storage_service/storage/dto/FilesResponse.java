package com.backend.storage_service.storage.dto;

import com.backend.storage_service.storage.model.FileStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class FilesResponse {

    private UUID id;

    private String bucket;

    private String originalName;

    private String contentType;

    private String extension;

    private Long size;

    private LocalDateTime createdAt;

    private LocalDateTime uploadedAt;

}
