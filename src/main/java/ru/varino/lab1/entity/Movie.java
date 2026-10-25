package ru.varino.lab1.entity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class Movie {
    private Integer id;

    @NotNull(message = "Название фильма обязательно.")
    @Size(min = 1, message = "Название фильма не должно быть пустым.")
    private String name;

    @Valid
    @NotNull(message = "Координаты обязательны.")
    private Coordinates coordinates;

    private LocalDateTime creationDate;

    @PositiveOrZero(message = "Количество премий «Оскар» должно быть неотрицательным.")
    private long oscarsCount;

    @Positive(message = "Бюджет должен быть больше нуля.")
    private Integer budget;

    @Positive(message = "Кассовые сборы должны быть больше нуля.")
    private Integer totalBoxOffice;

    @NotNull(message = "Возрастной рейтинг обязателен.")
    private MpaaRating mpaaRating;

    @Valid
    @NotNull(message = "Режиссёр обязателен.")
    private Person director;

    @Valid
    private Person screenwriter;

    @Valid
    @NotNull(message = "Оператор обязателен.")
    private Person operator;

    @Positive(message = "Продолжительность фильма должна быть больше нуля.")
    private Long length;

    @Positive(message = "Количество премий «Золотая пальмовая ветвь» должно быть больше нуля.")
    private Integer goldenPalmCount;

    private MovieGenre genre;
}
