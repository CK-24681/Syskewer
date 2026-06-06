package com.syskewer.api.dto.user;

import jakarta.validation.constraints.Email;

public record UserUpdateDto(
    String name,

    @Email(message = "O email deve ser válido")
    String email,

    Integer roleId
) {}
