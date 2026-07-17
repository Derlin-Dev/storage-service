package com.backend.storage_service.auth.model;

import com.backend.storage_service.storage.model.FileEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String name;
    private String email;
    private String pass;
    private boolean is_active;

    @OneToMany(
            mappedBy = "uploadedBy",
            cascade = CascadeType.ALL,
            orphanRemoval = false,
            fetch = FetchType.LAZY
    )
    private List<FileEntity> files = new ArrayList<>();

    public UserEntity(String name, String email, String pass, boolean is_active) {
        this.name = name;
        this.email = email;
        this.pass = pass;
        this.is_active = is_active;
    }


}
