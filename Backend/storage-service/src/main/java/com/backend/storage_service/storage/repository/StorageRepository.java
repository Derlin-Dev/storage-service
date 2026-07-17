package com.backend.storage_service.storage.repository;

import com.backend.storage_service.auth.model.UserEntity;
import com.backend.storage_service.storage.model.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StorageRepository extends JpaRepository<FileEntity, UUID> {

    List<FileEntity> findAllByUploadedById(UUID userId);

}
