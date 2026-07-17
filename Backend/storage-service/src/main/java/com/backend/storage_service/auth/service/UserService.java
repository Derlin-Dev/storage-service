package com.backend.storage_service.auth.service;

import com.backend.storage_service.auth.dto.UserResponse;
import com.backend.storage_service.auth.repository.UserRepository;
import com.backend.storage_service.auth.security.JwtServices;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final JwtServices jwtServices;

    public UserService(UserRepository userRepository, JwtServices jwtServices) {
        this.userRepository = userRepository;
        this.jwtServices = jwtServices;
    }

    public UserResponse getUser(){
        return new UserResponse();
    }

    public UserResponse updateInfoUser(){
        return new UserResponse();
    }

    public void deleteUser(){

    }
}
