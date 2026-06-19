package com.syskewer.api.dto.user;

public record UserResponseDto(
    Integer id,
    String name,
    String username,
    String email,
    String role,
    Boolean active
) {}
