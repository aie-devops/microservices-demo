package com.example.orderservice.dto;

// The shape of a user returned by GET /api/users/{id} on the User Service
public record UserDTO(Long id, String firstName, String lastName, String email) {
}
