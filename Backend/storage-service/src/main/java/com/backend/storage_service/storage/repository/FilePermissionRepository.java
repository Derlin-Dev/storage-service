package com.backend.storage_service.storage.repository;

import com.backend.storage_service.storage.model.FilePermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FilePermissionRepository extends JpaRepository<FilePermissionEntity, UUID> {

    boolean existsByFileIdAndUserId(UUID fileId, UUID userId);
    void deleteByFileIdAndUserId(UUID fileId, UUID userId);
}
