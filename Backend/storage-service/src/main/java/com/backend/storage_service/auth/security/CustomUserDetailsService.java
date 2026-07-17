package com.backend.storage_service.auth.security;

import com.backend.storage_service.auth.model.UserEntity;
import com.backend.storage_service.auth.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Optional<UserEntity> usersEntity = userRepository.findByEmail(email);

        if (usersEntity.isEmpty()){
            throw new UsernameNotFoundException("Usuario no encotrado");
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(usersEntity.get().getEmail())
                .password(usersEntity.get().getPass())
                .build();
    }
}
