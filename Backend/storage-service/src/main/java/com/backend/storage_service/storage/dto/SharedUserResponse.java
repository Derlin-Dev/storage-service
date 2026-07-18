package com.backend.storage_service.storage.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class SharedUserResponse {
    private UUID userId;
    private String email;
}
