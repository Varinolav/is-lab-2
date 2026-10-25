package ru.varino.lab1.entity;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Coordinates {
    private Long id;

    @NotNull(message = "Координата x обязательна.")
    @DecimalMin(value = "-59", inclusive = false, message = "Координата x должна быть больше -59.")
    private Double x;

    private float y;
}
