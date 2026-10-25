package ru.varino.lab1.exception;

public class UsernameAlreadyExistsException extends RuntimeException {
    public UsernameAlreadyExistsException() {
        super("Пользователь с таким логином уже существует.");
    }
}
