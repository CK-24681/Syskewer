package com.syskewer.api.dto.user;

import com.syskewer.api.model.user.Role;

public record UserResponseDto(
    Integer id,
    String name,
    String username,
    String email,
    Role role,
    Boolean active
) {}
