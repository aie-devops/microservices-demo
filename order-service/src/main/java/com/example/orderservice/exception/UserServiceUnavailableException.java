package com.example.orderservice.exception;

// The User Service could not be reached (stopped, wrong URL, or timed out)
public class UserServiceUnavailableException extends RuntimeException {

    public UserServiceUnavailableException(String userServiceUrl) {
        super("User Service is unavailable at " + userServiceUrl);
    }
}
