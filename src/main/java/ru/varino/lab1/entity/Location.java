package ru.varino.lab1.entity;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Location {
    private Long id;

    private int x;

    @NotNull(message = "Координата y местоположения обязательна.")
    private Double y;

    private double z;

    @Size(min = 1, message = "Название местоположения не должно быть пустым.")
    private String name;
}
