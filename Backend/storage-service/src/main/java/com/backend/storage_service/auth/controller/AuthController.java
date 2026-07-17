package com.backend.storage_service.auth.controller;

import com.backend.storage_service.auth.dto.AuthRequest;
import com.backend.storage_service.auth.dto.AuthResponse;
import com.backend.storage_service.auth.dto.RegisterRequest;
import com.backend.storage_service.auth.service.AuthService;
import com.backend.storage_service.utils.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/file-store/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registeNewUser(
           @RequestBody RegisterRequest registerRequest
    ){
        authService.registerNewUser(registerRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiResponse<>(
                        true,
                        "Usuario Registrado correctamente",
                        null
                ));

    }

        @PostMapping("/login")
        public ResponseEntity<?> login(
            @RequestBody AuthRequest authRequest
        ){
        AuthResponse authResponse = authService.login(authRequest);
        return ResponseEntity.status(HttpStatus.OK).body(
                new ApiResponse<>(
                        true,
                        "Inicio exitoso",
                        authResponse
                ));
    }
}
