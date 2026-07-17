package com.backend.storage_service.storage.service;

import com.backend.storage_service.exception.ForbiddenException;
import com.backend.storage_service.exception.UnauthorizedException;
import com.backend.storage_service.storage.model.FileEntity;
import com.backend.storage_service.storage.model.FileVisibility;
import com.backend.storage_service.storage.repository.FilePermissionRepository;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.UUID;

@Service
public class FilePermissionService {

    private final FilePermissionRepository filePermissionRepository;

    public FilePermissionService(FilePermissionRepository filePermissionRepository) {
        this.filePermissionRepository = filePermissionRepository;
    }

    public void checkOwnership(FileEntity file, UUID userId){
        // El dueño siempre tiene acceso, sin importar la visibilidad
        if (file.getUploadedBy().getId().equals(userId)) {
            return;
        }

        if (file.getVisibility() == FileVisibility.PUBLIC) {
            return;
        }

        if (file.getVisibility() == FileVisibility.PROTECTED
                && filePermissionRepository.existsByFileIdAndUserId(file.getId(), userId)) {
            return;
        }

        throw new ForbiddenException("No tienes permiso para acceder a este archivo");
    }

    public void checkPublicAccess(FileEntity file) {
        if (file.getVisibility() != FileVisibility.PUBLIC){
            throw new ForbiddenException("Este archivo no es publico");
        }
    }

    public void checkIsOwer(FileEntity file, UUID userId){
        if (!file.getUploadedBy().getId().equals(userId)) {
            throw new ForbiddenException("Solo el dueño del archivo puede realizar esta acción");
        }
    }
}
