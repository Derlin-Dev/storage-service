package com.backend.storage_service.storage.service;

import com.backend.storage_service.auth.model.UserEntity;
import com.backend.storage_service.auth.repository.UserRepository;
import com.backend.storage_service.exception.ForbiddenException;
import com.backend.storage_service.exception.ResourceNotFoundException;
import com.backend.storage_service.storage.dto.*;
import com.backend.storage_service.storage.model.FileEntity;
import com.backend.storage_service.storage.model.FilePermissionEntity;
import com.backend.storage_service.storage.model.FileStatus;
import com.backend.storage_service.storage.model.FileVisibility;
import com.backend.storage_service.storage.repository.FilePermissionRepository;
import com.backend.storage_service.storage.repository.StorageRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FileService {

    @Value("${storage.bucket}")
    private String bucket;

    @Value(("${app.base-url"))
    private String baseUrl;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    private final StorageRepository storageRepository;
    private final UserRepository userRepository;
    private final S3StorageProvider s3StorageProvider;
    private final FilePermissionService filePermissionService;
    private final FilePermissionRepository filePermissionRepository;

    public FileService(StorageRepository storageRepository, UserRepository userRepository, S3StorageProvider s3StorageProvider, FilePermissionService filePermissionService, FilePermissionRepository filePermissionRepository) {
        this.storageRepository = storageRepository;
        this.userRepository = userRepository;
        this.s3StorageProvider = s3StorageProvider;
        this.filePermissionService = filePermissionService;
        this.filePermissionRepository = filePermissionRepository;
    }

    public List<FilesResponse> getAllFilesByUser(UUID userid)throws ResourceNotFoundException{
        List<FileEntity> listFiles = storageRepository.findAllByUploadedById(userid);

        if (listFiles.isEmpty()) {
            throw new ResourceNotFoundException("Ningun documento encontrado");
        }

        List<FileEntity> files = new ArrayList<>();

        for(FileEntity file : listFiles){
            if (file.getStatus().equals(FileStatus.UPLOADED)){
                files.add(file);
            }
        }
        return files.stream()
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
    public DownloadResponse downloadFile(UUID fileId, UUID userId)throws ResourceNotFoundException{
        FileEntity file = storageRepository.findById(fileId).orElseThrow(() ->
                new ResourceNotFoundException("Archivo no encontrado")
        );

        filePermissionService.checkOwnership(file, userId);

        String downloadUrl = s3StorageProvider.generateDownloadUrl(file);

        return new DownloadResponse(
                file.getId(),
                file.getOriginalName(),
                file.getContentType(),
                file.getSize(),
                downloadUrl
        );

    }

    @Transactional
    public void confirmUploadFile(UUID fileId, UUID userId){

        FileEntity file = storageRepository.findById(fileId).orElseThrow( () ->
                new ResourceNotFoundException("Archivo no encontrado"));

        filePermissionService.checkOwnership(file, userId);

        file.setStatus(FileStatus.UPLOADED);

        storageRepository.save(file);
    }

    @Transactional
    public void deleteFile(UUID fileId, UUID userId){
        FileEntity file = storageRepository.findById(fileId).orElseThrow( () ->
                new ResourceNotFoundException("Archivo no encontrado"));

        filePermissionService.checkOwnership(file, userId);

        if (s3StorageProvider.exists(file)){
            s3StorageProvider.delete(file);
        }

        storageRepository.delete(file);
    }

    @Transactional
    public void updateVisibilityPublicFile(UUID fileId, UUID userId){
        FileEntity file = storageRepository.findById(fileId).orElseThrow( () ->
                new ResourceNotFoundException("Archivo no encontrado"));

        filePermissionService.checkIsOwner(file, userId);

        file.setVisibility(FileVisibility.PUBLIC);

        if (file.getShareToken() == null){
            file.setShareToken(
                    UUID.randomUUID().toString().replace("-", "")
            );
        }

        storageRepository.save(file);
    }

    public String downloadFilePublic(String token){
        FileEntity file = storageRepository.findByShareToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Archivo no encontrado"));

        if (file.getVisibility() != FileVisibility.PUBLIC){
            throw new ForbiddenException("Este archivo no es publico");
        }

        return s3StorageProvider.generateDownloadUrl(file);
    }

    public String getShareLink(UUID fileId, UUID userId){
        FileEntity file = storageRepository.findById(fileId).orElseThrow( () ->
                new ResourceNotFoundException("Archivo no encontrado"));

        filePermissionService.checkIsOwner(file, userId);

        if (file.getVisibility() != FileVisibility.PUBLIC){
            updateVisibilityPublicFile(fileId, userId);
        }
        return baseUrl + "/file-store/api/v1/files/share/" + file.getShareToken();
    }

    @Transactional
    public String grantAccess(UUID fileId, UUID owenId, String granteeEmail){
        FileEntity file = storageRepository.findById(fileId).orElseThrow( () ->
                new ResourceNotFoundException("Archivo no encontrado"));

        filePermissionService.checkIsOwner(file, owenId);

        UserEntity grantee = userRepository.findByEmail(granteeEmail).orElseThrow(() ->
                new ResourceNotFoundException("No existe una cuenta con ese correo. Pídele a la persona que se registre primero.")
        );

        if (grantee.getId().equals(owenId)){
            throw new IllegalArgumentException("No puedes compartir un archivo contigo mismo");
        }

        if (!filePermissionRepository.existsByFileIdAndUserId(fileId, grantee.getId())) {
            if (file.getVisibility() != FileVisibility.PROTECTED) {
                file.setVisibility(FileVisibility.PROTECTED);
                storageRepository.save(file);
            }
            filePermissionRepository.save(new FilePermissionEntity(file, grantee));
        }

        return frontendUrl + "/files/" + file.getId();

    }

    @Transactional
    public void revokeAccess(UUID fileId, UUID ownerId, UUID granteeUserId) {
        FileEntity file = storageRepository.findById(fileId).orElseThrow(() ->
                new ResourceNotFoundException("Archivo no encontrado"));

        filePermissionService.checkIsOwner(file, ownerId);

        filePermissionRepository.deleteByFileIdAndUserId(fileId, granteeUserId);

        if (file.getVisibility() == FileVisibility.PROTECTED
                && filePermissionRepository.countByFileId(fileId) == 0) {
            file.setVisibility(FileVisibility.PRIVATE);
            storageRepository.save(file);
        }
    }

    public List<SharedUserResponse> getUsersWithAccess(UUID fileId, UUID ownerId) {
        FileEntity file = storageRepository.findById(fileId).orElseThrow(() ->
                new ResourceNotFoundException("Archivo no encontrado"));

        filePermissionService.checkIsOwner(file, ownerId);

        return filePermissionRepository.findAllByFileId(fileId).stream()
                .map(p -> new SharedUserResponse(p.getUser().getId(), p.getUser().getEmail()))
                .toList();
    }

    private String getExtension(String fileName) {

        int index = fileName.lastIndexOf('.');

        if (index == -1) {
            return "";
        }

        return fileName.substring(index + 1);
    }
}
