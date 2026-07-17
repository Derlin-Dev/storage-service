package com.backend.storage_service.exception.dtoException;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ErrorMessage {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
}
