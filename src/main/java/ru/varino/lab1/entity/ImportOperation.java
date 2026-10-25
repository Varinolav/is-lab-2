package ru.varino.lab1.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ImportOperation {
    private Long id;
    private String username;
    private ImportStatus status;
    private LocalDateTime creationDate;
    private Long count;
}
