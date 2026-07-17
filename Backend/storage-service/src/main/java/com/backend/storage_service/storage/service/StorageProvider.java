package com.backend.storage_service.storage.service;

import com.backend.storage_service.storage.model.FileEntity;

public interface StorageProvider {

    String generateUploadUrl(FileEntity file);

    String generateDownloadUrl(FileEntity file);

    void delete(FileEntity file);

    boolean exists(FileEntity file);

}
