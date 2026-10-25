package ru.varino.lab1.entity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Person {
    private Long id;

    @NotNull(message = "Имя обязательно.")
    @Size(min = 1, message = "Имя не должно быть пустым.")
    private String name;

    private Color eyeColor;

    private Color hairColor;

    @Valid
    @NotNull(message = "Местоположение обязательно.")
    private Location location;

    @Positive(message = "Рост должен быть больше нуля.")
    private Double height;

    private Country nationality;
}
