package ru.varino.lab1.exception;

public class MovieNameConflictException extends RuntimeException {
    public MovieNameConflictException(String name) {
        super("Название фильма «" + name + "» уже используется.");
    }
}
