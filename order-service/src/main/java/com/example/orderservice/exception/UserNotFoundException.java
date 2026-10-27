package com.example.orderservice.exception;

// The User Service answered, but the user does not exist (HTTP 404)
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long userId) {
        super("User not found with ID: " + userId);
    }
}
