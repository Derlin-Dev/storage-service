package com.backend.storage_service.auth.service;

import com.backend.storage_service.auth.dto.AuthRequest;
import com.backend.storage_service.auth.dto.AuthResponse;
import com.backend.storage_service.auth.dto.RegisterRequest;
import com.backend.storage_service.auth.model.UserEntity;
import com.backend.storage_service.auth.repository.UserRepository;
import com.backend.storage_service.auth.security.JwtServices;
import com.backend.storage_service.exception.ExistingRecordException;
import com.backend.storage_service.exception.UnauthorizedException;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JwtServices jwtServices;

    public AuthService(PasswordEncoder passwordEncoder, UserRepository userRepository, JwtServices jwtServices) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.jwtServices = jwtServices;
    }

    @Transactional
    public AuthResponse login(AuthRequest request) throws UnauthorizedException{
        Optional<UserEntity> userOp = userRepository.findByEmail(request.getEmail());

        if (userOp.isEmpty() || !passwordEncoder.matches(request.getPass(), userOp.get().getPass())){
            throw new UnauthorizedException("Credenciales invalidas");
        }

        UserEntity userEntity = userOp.get();

        String token = jwtServices.generateTokeJwt(
            userEntity
        );

        return new AuthResponse(token);
    }

    public void registerNewUser(RegisterRequest registerRequest) throws ExistingRecordException {

        boolean emailVerified = userRepository.existsEmail(registerRequest.getEmail());

        if(emailVerified) {
            throw new ExistingRecordException("Email ya registrado.");
        }

        UserEntity newUser = new UserEntity(
                registerRequest.getName(),
                registerRequest.getEmail(),
                passwordEncoder.encode(registerRequest.getPass()),
                true
        );

        userRepository.save(newUser);
    }

}
