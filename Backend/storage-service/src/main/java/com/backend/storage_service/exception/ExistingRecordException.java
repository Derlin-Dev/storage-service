package com.backend.storage_service.exception;

public class ExistingRecordException extends RuntimeException {
    public ExistingRecordException(String message) {
        super(message);
    }
}
