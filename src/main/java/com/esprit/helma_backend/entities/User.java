package com.esprit.helma_backend.entities;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity

@Table(name = "users")
public class User {

    public enum Role {
        ADMIN, USER
    }


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "is_entrepreneur", nullable = false)
    private Boolean isEntrepreneur = false;

    @Column(nullable = false)
    private String password;



}
