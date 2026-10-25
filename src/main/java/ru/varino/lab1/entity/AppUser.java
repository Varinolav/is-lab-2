package ru.varino.lab1.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AppUser {
    private String username;
    private String passwordHash;
    private String role;
}
