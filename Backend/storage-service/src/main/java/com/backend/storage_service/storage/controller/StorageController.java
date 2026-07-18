package com.backend.storage_service.storage.controller;

import com.backend.storage_service.storage.dto.*;
import com.backend.storage_service.storage.service.FileService;
import com.backend.storage_service.utils.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.backend.storage_service.storage.dto.GrantAccessRequest;
import com.backend.storage_service.storage.dto.SharedUserResponse;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/file-store/api/v1/files")
public class StorageController {

    private final FileService fileService;

    public StorageController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload-request")
    public ResponseEntity<?> UploadFileRequest(
            HttpServletRequest servletRequest,
            @RequestBody UploadRequest uploadRequest
    ){
        UUID userId = (UUID) servletRequest.getAttribute("userId");

        UploadResponse response = fileService.uploadFile(uploadRequest, userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiResponse<>(
                        true,
                        "Url generada correctamente",
                        response
                ));
    }

    @GetMapping()
    public ResponseEntity<?> getAllFilesByUser(
            HttpServletRequest servletRequest
    ){
        UUID userId = (UUID) servletRequest.getAttribute("userId");

        List<FilesResponse> filesResponses = fileService.getAllFilesByUser(userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiResponse<>(
                        true,
                        "Archivos encontrados",
                        filesResponses
                ));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<?> downloadFile(
            @PathVariable String id,
            HttpServletRequest servletRequest
    ){
        UUID userId = (UUID) servletRequest.getAttribute("userId");
        UUID fileId = UUID.fromString(id);

        DownloadResponse downloadResponse = fileService.downloadFile(fileId, userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiResponse<>(
                        true,
                        "Archivos encontrados",
                        downloadResponse
                ));
    }

    @PutMapping("/{id}/confirm-upload")
    public ResponseEntity<?> confirmUploadFile(
            @PathVariable String id,
            HttpServletRequest servletRequest
    ){
        UUID userId = (UUID) servletRequest.getAttribute("userId");
        UUID fileId = UUID.fromString(id);

        fileService.confirmUploadFile(fileId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiResponse<>(
                        true,
                        "Estatus actualizado",
                        null
                ));
    }

    @DeleteMapping("/{id}/delete")
    public ResponseEntity<?> deleteFile(
            @PathVariable String id,
            HttpServletRequest servletRequest
    ){
        UUID userId = (UUID) servletRequest.getAttribute("userId");
        UUID fileId = UUID.fromString(id);

        fileService.deleteFile(fileId, userId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(
                new ApiResponse<>(
                        true,
                        "Archivo eliminado correctamente",
                        null
                ));
    }

    @PutMapping("{id}/update-permission")
    public ResponseEntity<?> updatePermissionPublic(
            @PathVariable String id,
            HttpServletRequest servletRequest
    ){
        UUID userId = (UUID) servletRequest.getAttribute("userId");
        UUID fileId = UUID.fromString(id);

        fileService.updateVisibilityPublicFile(fileId, userId);
        return ResponseEntity.status(HttpStatus.OK).body(
                new ApiResponse<>(
                        true,
                        "Visibilidad del documento actualizada",
                        null
                ));
    }

    @GetMapping("/{id}/get-sharelink")
    public ResponseEntity<?> getShareLink(
            @PathVariable String id,
            HttpServletRequest servletRequest
    ){
        UUID userId = (UUID) servletRequest.getAttribute("userId");
        UUID fileId = UUID.fromString(id);

        String url = fileService.getShareLink(fileId, userId);

        return ResponseEntity.status(HttpStatus.OK).body(
                new ApiResponse<>(
                        true,
                        "Url publica creada correctamente",
                        url
                ));
    }

    @GetMapping("/share/{token}")
    public ResponseEntity<?> downloadSharedFile(@PathVariable String token){
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(fileService.downloadFilePublic(token)))
                .build();
    }

    @PostMapping("/{id}/share-with")
    public ResponseEntity<?> grantAccess(
            @PathVariable String id,
            @RequestBody GrantAccessRequest request,
            HttpServletRequest servletRequest
    ){
        UUID userId = (UUID) servletRequest.getAttribute("userId");
        String link = fileService.grantAccess(UUID.fromString(id), userId, request.getEmail());

        return ResponseEntity.ok(new ApiResponse<>(true, "Acceso otorgado", link));
    }

    @DeleteMapping("/{id}/share-with/{granteeUserId}")
    public ResponseEntity<?> revokeAccess(
            @PathVariable String id,
            @PathVariable String granteeUserId,
            HttpServletRequest servletRequest
    ){
        UUID userId = (UUID) servletRequest.getAttribute("userId");
        fileService.revokeAccess(UUID.fromString(id), userId, UUID.fromString(granteeUserId));

        return ResponseEntity.ok(new ApiResponse<>(true, "Acceso revocado", null));
    }

    @GetMapping("/{id}/shared-with")
    public ResponseEntity<?> getUsersWithAccess(
            @PathVariable String id,
            HttpServletRequest servletRequest
    ){
        UUID userId = (UUID) servletRequest.getAttribute("userId");
        List<SharedUserResponse> users = fileService.getUsersWithAccess(UUID.fromString(id), userId);

        return ResponseEntity.ok(new ApiResponse<>(true, "Usuarios con acceso", users));
    }
}
