package ru.varino.lab1.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "movie_name_registry")
@Getter
@NoArgsConstructor
public class MovieNameRegistry {
    public static final long ID = 1L;

    @Id
    private Long id = ID;

    @Version
    private long version;
}
