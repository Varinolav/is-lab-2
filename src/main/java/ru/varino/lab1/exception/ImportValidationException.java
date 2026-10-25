package ru.varino.lab1.exception;

public class ImportValidationException extends RuntimeException {
    public ImportValidationException(String message) {
        super(message);
    }
}
