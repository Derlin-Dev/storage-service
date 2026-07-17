package com.backend.storage_service.storage.model;

import com.backend.storage_service.auth.model.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "file_permissions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"file_id", "user_id"})
}
)
@Getter
@Setter
@NoArgsConstructor
public class FilePermissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private FileEntity file;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @CreationTimestamp
    private LocalDateTime grantedAt;

    public FilePermissionEntity(FileEntity file, UserEntity user) {
        this.file = file;
        this.user = user;
    }
}
