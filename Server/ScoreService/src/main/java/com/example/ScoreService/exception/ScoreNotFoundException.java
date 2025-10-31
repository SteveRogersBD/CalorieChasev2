package com.example.ScoreService.exception;

public class ScoreNotFoundException extends RuntimeException {
    public ScoreNotFoundException(String message) {
        super(message);
    }
    public ScoreNotFoundException(String message,Throwable cause) {
        super(message,cause);
    }
}
