package com.backend.storage_service.auth.repository;

import com.backend.storage_service.auth.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String email);

    @Query("""
        SELECT CASE
            WHEN COUNT(e) > 0 THEN true
            ELSE false
        END
        FROM UserEntity e
        WHERE e.email = :email
    """)
    boolean existsEmail(@Param("email") String email);

}
