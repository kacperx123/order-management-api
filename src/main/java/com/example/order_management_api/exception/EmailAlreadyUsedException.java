package com.example.order_management_api.exception;

public class EmailAlreadyUsedException extends RuntimeException {
    public EmailAlreadyUsedException(String email) {
        super("Email already in use: " + email);
    }
}
