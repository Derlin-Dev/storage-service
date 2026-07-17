package com.backend.storage_service.storage.service;

import com.backend.storage_service.auth.model.UserEntity;
import com.backend.storage_service.auth.repository.UserRepository;
import com.backend.storage_service.exception.ResourceNotFoundException;
import com.backend.storage_service.storage.dto.DownloadResponse;
import com.backend.storage_service.storage.dto.FilesResponse;
import com.backend.storage_service.storage.dto.UploadRequest;
import com.backend.storage_service.storage.dto.UploadResponse;
import com.backend.storage_service.storage.model.FileEntity;
import com.backend.storage_service.storage.model.FileStatus;
import com.backend.storage_service.storage.repository.StorageRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FileService {

    @Value("${storage.bucket}")
    private String bucket;

    private final StorageRepository storageRepository;
    private final UserRepository userRepository;
    private final S3StorageProvider s3StorageProvider;

    public FileService(StorageRepository storageRepository, UserRepository userRepository, S3StorageProvider s3StorageProvider) {
        this.storageRepository = storageRepository;
        this.userRepository = userRepository;
        this.s3StorageProvider = s3StorageProvider;
    }

    public List<FilesResponse> getAllFilesByUser(UUID userid)throws ResourceNotFoundException{
        List<FileEntity> listFiles = storageRepository.findAllByUploadedById(userid);

        if (listFiles.isEmpty()) {
            throw new ResourceNotFoundException("Ningun documento encontrado");
        }
        return listFiles.stream()
                .map(file -> new FilesResponse(
                        file.getId(),
                        file.getBucket(),
                        file.getOriginalName(),
                        file.getContentType(),
                        file.getExtension(),
                        file.getSize(),
                        file.getCreatedAt(),
                        file.getUploadedAt()
                ))
                .toList();
    }

    @Transactional
    public UploadResponse uploadFile(UploadRequest uploadRequest, UUID userId)throws ResourceNotFoundException {

        Optional<UserEntity> user = userRepository.findById(userId);

        if (user.isEmpty()) {
            throw new ResourceNotFoundException("Usuario no encontrado");
        }

        String extension = getExtension(uploadRequest.getFilename());

        String objectKey = String.format(
                "upload/%s.%s",
                UUID.randomUUID(),
                extension
        );

        FileEntity file = new FileEntity(
                bucket,
                objectKey,
                uploadRequest.getFilename(),
                uploadRequest.getContentType(),
                extension,
                uploadRequest.getSize(),
                FileStatus.PENDING_UPLOAD,
                user.orElse(null)
        );

        file = storageRepository.save(file);

        String uploadUrl = s3StorageProvider.generateUploadUrl(file);
        return new UploadResponse(
                file.getId(),
                uploadUrl,
                objectKey
        );
    }

    @Transactional()
    public DownloadResponse downloadFile(UUID fileId)throws ResourceNotFoundException{
        FileEntity file = storageRepository.findById(fileId).orElseThrow(() ->
                new ResourceNotFoundException("Archivo no encontrado")
        );

        String downloadUrl = s3StorageProvider.generateDownloadUrl(file);

        return new DownloadResponse(
                file.getId(),
                file.getOriginalName(),
                file.getContentType(),
                file.getSize(),
                downloadUrl
        );

    }

    private String getExtension(String fileName) {

        int index = fileName.lastIndexOf('.');

        if (index == -1) {
            return "";
        }

        return fileName.substring(index + 1);
    }
}
